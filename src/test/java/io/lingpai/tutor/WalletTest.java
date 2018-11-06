package io.lingpai.tutor;

import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;

import static junit.framework.TestCase.assertEquals;

public class WalletTest {

    static {
        //必须设置才能使用BouncyCastle的椭圆算法来签名
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }
    @Test
    public void publicKeyBackup() throws InvalidKeySpecException, NoSuchAlgorithmException {
        Wallet wallet = new Wallet();
        String originalKeyString = Utils.getStringFromKey(wallet.publicKey);

        PublicKey publicKey = Utils.getPublicKeyFromString(originalKeyString);

        assertEquals(wallet.publicKey, publicKey);

        assertEquals(originalKeyString, Utils.getStringFromKey(publicKey));
    }
}
