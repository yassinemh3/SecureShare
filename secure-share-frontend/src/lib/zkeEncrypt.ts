export async function encryptFile(file: File, passphrase: string): Promise<Blob> {
  const encoder = new TextEncoder();
  const passphraseBytes = encoder.encode(passphrase);
  const iv = crypto.getRandomValues(new Uint8Array(12)); // MUST BE 12 BYTES for AES-GCM

  const keyMaterial = await crypto.subtle.importKey(
    "raw",
    passphraseBytes,
    { name: "PBKDF2" },
    false,
    ["deriveKey"]
  );

  const key = await crypto.subtle.deriveKey(
    {
      name: "PBKDF2",
      salt: iv, // same salt as IV
      iterations: 100000,
      hash: "SHA-256"
    },
    keyMaterial,
    { name: "AES-GCM", length: 256 },
    false,
    ["encrypt"]
  );

  const fileBuffer = await file.arrayBuffer();

  const encrypted = await crypto.subtle.encrypt(
    { name: "AES-GCM", iv },
    key,
    fileBuffer
  );

  // 🔁 Prepend IV to encrypted data
  const result = new Uint8Array(iv.length + encrypted.byteLength);
  result.set(iv, 0);
  result.set(new Uint8Array(encrypted), iv.length);

  return new Blob([result], { type: "application/octet-stream" });
}
