package co.cyte.agent.core.crypto;

public interface EncryptionAlgorithm {
    /**
     * Cifra un texto plano dado
     *
     * @param plainText el contenido original
     * @return el contenido cifrado
     */
    String encrypt(String plainText);

    /**
     * Cifra un texto cifrado dado
     *
     * @param cipherText el contenido cifrado
     * @return el contenido descifrado
     */
    String decrypt(String cipherText);
}


