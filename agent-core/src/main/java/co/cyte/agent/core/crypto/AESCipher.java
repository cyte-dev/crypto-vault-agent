package co.cyte.agent.core.crypto;

public class AESCipher implements EncryptionAlgorithm {

    @Override
    public String encrypt(String plainText) {
        // Si ya está cifrado, retorna el contenido sin cambios.
        if (plainText.startsWith("ARCHIVO CIFRADO")) {
            return plainText;
        }
        // Simula el cifrado anteponiendo la línea "ARCHIVO CIFRADO"
        return "ARCHIVO CIFRADO\n" + plainText;
    }

    @Override
    public String decrypt(String cipherText) {
        // Si el contenido inicia con "ARCHIVO CIFRADO", lo reemplaza por "ARCHIVO DESCIFRADO"
        if (cipherText.startsWith("ARCHIVO CIFRADO")) {
            int newlineIndex = cipherText.indexOf("\n");
            if (newlineIndex != -1) {
                String rest = cipherText.substring(newlineIndex + 1);
                return "ARCHIVO DESCIFRADO\n" + rest;
            } else {
                return "ARCHIVO DESCIFRADO";
            }
        }
        // Si no está cifrado, retorna el contenido original.
        return cipherText;
    }
}
