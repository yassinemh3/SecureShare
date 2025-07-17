export async function decryptFile(encryptedData: ArrayBuffer, passphrase: string, iv: Uint8Array): Promise<ArrayBuffer> {
  const encoder = new TextEncoder();
  const passphraseBytes = encoder.encode(passphrase);

  // Derive a key from the passphrase
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
      salt: iv, // Salt should be consistent with what you used in encryption
      iterations: 100000,
      hash: "SHA-256"
    },
    keyMaterial,
    { name: "AES-GCM", length: 256 },
    false,
    ["decrypt"]
  );

  // Decrypt
  return await crypto.subtle.decrypt(
    { name: "AES-GCM", iv },
    key,
    encryptedData
  );
}
