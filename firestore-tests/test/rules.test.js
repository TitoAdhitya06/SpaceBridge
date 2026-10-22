const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const {
  doc,
  collection,
  setDoc,
  getDoc,
  getDocs,
  updateDoc,
  deleteDoc,
  serverTimestamp,
} = require("firebase/firestore");
const fs = require("fs");
const path = require("path");

const PROJECT_ID = "spacebridge-rules-test";

describe("Firestore Security Rules - SpaceBridge v3", function () {
  this.timeout(10000);

  let testEnv;

  before(async () => {
    const rulesPath = path.resolve(__dirname, "../../firestore.rules");
    const rules = fs.readFileSync(rulesPath, "utf8");

    testEnv = await initializeTestEnvironment({
      projectId: PROJECT_ID,
      firestore: {
        rules: rules,
        host: "127.0.0.1",
        port: 8080,
      },
    });
  });

  after(async () => {
    if (testEnv) {
      await testEnv.cleanup();
    }
  });

  beforeEach(async () => {
    if (testEnv) {
      await testEnv.clearFirestore();
    }
  });

  describe("1. Isolasi Akun: Pengguna A vs Pengguna B", () => {
    it("Pengguna A tidak dapat membaca profil, catatan, berkas, atau label Pengguna B", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      await assertFails(getDoc(doc(aliceDb, "users/bob")));
      await assertFails(getDoc(doc(aliceDb, "users/bob/notes/note-1")));
      await assertFails(getDoc(doc(aliceDb, "users/bob/files/file-1")));
      await assertFails(getDoc(doc(aliceDb, "users/bob/labels/label-1")));
    });

    it("Pengguna A tidak dapat menulis ke path profil, catatan, berkas, atau label Pengguna B", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const noteData = {
        type: "text",
        title: "Catatan Bob",
        body: "Isi catatan",
        items: [],
        pinned: false,
        labelIds: [],
        attachmentIds: [],
        status: "active",
        trashedAt: null,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertFails(setDoc(doc(aliceDb, "users/bob"), { displayName: "Hacked" }));
      await assertFails(setDoc(doc(aliceDb, "users/bob/notes/note-hacked"), noteData));
      await assertFails(setDoc(doc(aliceDb, "users/bob/files/file-hacked"), { name: "test.pdf" }));
      await assertFails(setDoc(doc(aliceDb, "users/bob/labels/label-hacked"), { name: "Penting" }));
    });

    it("Pengguna A tidak dapat menghapus catatan milik Pengguna B", async () => {
      const bobDb = testEnv.authenticatedContext("bob").firestore();
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const noteData = {
        type: "text",
        title: "Catatan Rahasia Bob",
        body: "Rahasia",
        items: [],
        pinned: false,
        labelIds: [],
        attachmentIds: [],
        status: "active",
        trashedAt: null,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertSucceeds(setDoc(doc(bobDb, "users/bob/notes/note-bob"), noteData));

      // Alice mencoba menghapus data Bob -> harus ditolak
      await assertFails(deleteDoc(doc(aliceDb, "users/bob/notes/note-bob")));

      // Bob dapat menghapus catatannya sendiri
      await assertSucceeds(deleteDoc(doc(bobDb, "users/bob/notes/note-bob")));
    });
  });

  describe("2. Validasi Skema Catatan (notes)", () => {
    it("Pembuatan catatan dengan skema tidak lengkap atau field hilang ditolak", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const incompleteNote = {
        type: "text",
        title: "Judul Catatan",
        body: "Isi",
        items: [],
        pinned: false,
        labelIds: [],
        attachmentIds: [],
        status: "active",
        trashedAt: null,
        createdAt: serverTimestamp(),
      };

      await assertFails(setDoc(doc(aliceDb, "users/alice/notes/note-incomplete"), incompleteNote));
    });

    it("Pembuatan catatan dengan field type tidak valid ditolak", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const invalidTypeNote = {
        type: "audio",
        title: "Audio note",
        body: "Konten audio",
        items: [],
        pinned: false,
        labelIds: [],
        attachmentIds: [],
        status: "active",
        trashedAt: null,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertFails(setDoc(doc(aliceDb, "users/alice/notes/note-invalid-type"), invalidTypeNote));
    });

    it("Pembuatan catatan dengan status tidak valid ditolak", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const invalidStatusNote = {
        type: "text",
        title: "Status note",
        body: "Konten",
        items: [],
        pinned: false,
        labelIds: [],
        attachmentIds: [],
        status: "deleted",
        trashedAt: null,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertFails(setDoc(doc(aliceDb, "users/alice/notes/note-invalid-status"), invalidStatusNote));
    });

    it("Pembuatan catatan dengan skema lengkap dan valid diizinkan untuk pemilik", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const validNote = {
        type: "text",
        title: "Rencana Liburan",
        body: "Pergi ke pantai",
        items: [],
        pinned: false,
        labelIds: [],
        attachmentIds: [],
        status: "active",
        trashedAt: null,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertSucceeds(setDoc(doc(aliceDb, "users/alice/notes/note-valid"), validNote));
    });

    it("Pembaruan catatan mempertahankan createdAt yang tidak berubah", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const initialNote = {
        type: "text",
        title: "Judul Awal",
        body: "Isi awal",
        items: [],
        pinned: false,
        labelIds: [],
        attachmentIds: [],
        status: "active",
        trashedAt: null,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertSucceeds(setDoc(doc(aliceDb, "users/alice/notes/note-update"), initialNote));

      await assertSucceeds(
        updateDoc(doc(aliceDb, "users/alice/notes/note-update"), {
          title: "Judul Diperbarui",
          updatedAt: serverTimestamp(),
        })
      );

      await assertFails(
        updateDoc(doc(aliceDb, "users/alice/notes/note-update"), {
          createdAt: serverTimestamp(),
          updatedAt: serverTimestamp(),
        })
      );
    });
  });

  describe("3. Validasi Skema Berkas (files)", () => {
    it("Pembuatan berkas valid dengan 17 field exact diizinkan", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const validFile = {
        name: "Laporan.pdf",
        mimeType: "application/pdf",
        sizeBytes: 1024000,
        category: "document",
        source: "drive",
        driveFileId: "drive-id-123",
        webViewLink: "https://drive.google.com/file/d/123/view",
        externalUrl: null,
        md5Checksum: null,
        status: "ready",
        uploadDeviceId: "device-uuid",
        pendingDriveOp: null,
        shared: false,
        sharePermissionId: null,
        trashedAt: null,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertSucceeds(setDoc(doc(aliceDb, "users/alice/files/file-valid"), validFile));
    });

    it("Pembuatan berkas berukuran besar (misal 5 GB = 5368709120 bytes) diizinkan tanpa batas artifisial", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const largeFile = {
        name: "VideoBesar.mp4",
        mimeType: "video/mp4",
        sizeBytes: 5368709120, // 5 GB
        category: "other",
        source: "drive",
        driveFileId: null,
        webViewLink: null,
        externalUrl: null,
        md5Checksum: null,
        status: "uploading",
        uploadDeviceId: null,
        pendingDriveOp: null,
        shared: false,
        sharePermissionId: null,
        trashedAt: null,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertSucceeds(setDoc(doc(aliceDb, "users/alice/files/file-large"), largeFile));
    });

    it("Pembuatan berkas dengan sizeBytes bernilai negatif ditolak", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();

      const negativeSizeFile = {
        name: "Corrupt.bin",
        mimeType: "application/octet-stream",
        sizeBytes: -1,
        category: "other",
        source: "drive",
        driveFileId: null,
        webViewLink: null,
        externalUrl: null,
        md5Checksum: null,
        status: "uploading",
        uploadDeviceId: null,
        pendingDriveOp: null,
        shared: false,
        sharePermissionId: null,
        trashedAt: null,
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertFails(setDoc(doc(aliceDb, "users/alice/files/file-negative"), negativeSizeFile));
    });
  });

  describe("4. Akses Pengguna Tanpa Autentikasi (Unauthenticated)", () => {
    it("Pengguna tanpa autentikasi ditolak seluruhnya saat membaca data apa pun", async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();

      await assertFails(getDoc(doc(unauthDb, "users/alice")));
      await assertFails(getDoc(doc(unauthDb, "users/alice/notes/note-1")));
      await assertFails(getDoc(doc(unauthDb, "users/alice/files/file-1")));
      await assertFails(getDoc(doc(unauthDb, "users/alice/labels/label-1")));
    });

    it("Pengguna tanpa autentikasi ditolak seluruhnya saat menulis data apa pun", async () => {
      const unauthDb = testEnv.unauthenticatedContext().firestore();

      await assertFails(
        setDoc(doc(unauthDb, "users/alice/notes/note-unauth"), {
          title: "Catatan Anonim",
        })
      );
    });
  });

  describe("5. Proteksi Profil Pengguna & Anti-Enumerasi", () => {
    it("Membaca daftar koleksi users (list) ditolak untuk semua pengguna", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      await assertFails(getDocs(collection(aliceDb, "users")));
    });

    it("Pembuatan profil pengguna dengan 8 field wajib diizinkan", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      const validProfile = {
        displayName: "Alice Wonderland",
        email: "alice@example.com",
        photoUrl: "https://example.com/alice.jpg",
        driveFolderId: "drive-folder-123",
        migratedV3At: null,
        createdAt: serverTimestamp(),
        lastSeenAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertSucceeds(setDoc(doc(aliceDb, "users/alice"), validProfile));
    });

    it("Pembuatan profil pengguna dengan field tambahan tidak dikenal ditolak", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      const profileWithExtraField = {
        displayName: "Alice Wonderland",
        email: "alice@example.com",
        photoUrl: null,
        driveFolderId: null,
        migratedV3At: null,
        createdAt: serverTimestamp(),
        lastSeenAt: serverTimestamp(),
        schemaVersion: 1,
        role: "admin",
      };

      await assertFails(setDoc(doc(aliceDb, "users/alice"), profileWithExtraField));
    });
  });

  describe("6. Validasi Label dan Migrasi Catatan Lama", () => {
    it("Pembuatan label yang valid diizinkan untuk pemilik", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      const validLabel = {
        name: "Proyek Pribadi",
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertSucceeds(setDoc(doc(aliceDb, "users/alice/labels/label-1"), validLabel));
    });

    it("Pembuatan label dengan nama lebih dari 40 karakter ditolak", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      const invalidLabel = {
        name: "A".repeat(41),
        createdAt: serverTimestamp(),
        updatedAt: serverTimestamp(),
        schemaVersion: 1,
      };

      await assertFails(setDoc(doc(aliceDb, "users/alice/labels/label-long"), invalidLabel));
    });

    it("Penulisan baru ke koleksi legacy 'catatan' ditolak untuk semua pengguna", async () => {
      const aliceDb = testEnv.authenticatedContext("alice").firestore();
      await assertFails(
        setDoc(doc(aliceDb, "users/alice/catatan/legacy-note"), {
          judul: "Catatan Lama",
          isi: "Isi lama",
        })
      );
    });
  });
});
