package com.linkbridge.core.security
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class CryptoEngine @Inject constructor(){
 private val alias="linkbridge_aes_256"
 private fun key():SecretKey { val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}; return (ks.getKey(alias,null) as? SecretKey) ?: KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run { init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setRandomizedEncryptionRequired(true).build()); generateKey() } }
 fun encrypt(data:ByteArray,aad:ByteArray=byteArrayOf()):ByteArray { val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.ENCRYPT_MODE,key());c.updateAAD(aad);return c.iv+c.doFinal(data) }
 fun decrypt(packet:ByteArray,aad:ByteArray=byteArrayOf()):ByteArray { require(packet.size>28);val c=Cipher.getInstance("AES/GCM/NoPadding");c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,packet.copyOfRange(0,12)));c.updateAAD(aad);return c.doFinal(packet.copyOfRange(12,packet.size)) }
}
