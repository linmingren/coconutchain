package io.lingpai.tutor;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.bouncycastle.asn1.pkcs.RSAPublicKey;

import java.io.IOException;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;

public class Utils {

    //对输入的字符串进行sha256哈希，返回16进制的字符串
    public static String applySha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            //Applies sha256 to our input,
            byte[] hash = digest.digest(input.getBytes("UTF-8"));
            StringBuffer hexString = new StringBuffer(); // This will contain hash as hexidecimal
            for (int i = 0; i < hash.length; i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJson(Object o) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        //PublicKey和PrivateKey两种类型默认无法打印成json，需要自定义处理
        SimpleModule module = new SimpleModule();
        module.addSerializer(Key.class, new ItemSerializer());
        mapper.registerModule(module);


        //美化输出
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        return mapper.writeValueAsString(o);
    }

    public static <T extends Object> T fromJson(String source, Class<T> valueType) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(source, valueType);
    }

    //用椭圆算法对input进行签名
    public static byte[] applyECDSASig(PrivateKey privateKey, String input) {
        Signature dsa;
        byte[] output = new byte[0];
        try {
            dsa = Signature.getInstance("ECDSA", "BC");
            dsa.initSign(privateKey);
            byte[] strByte = input.getBytes();
            dsa.update(strByte);
            byte[] realSig = dsa.sign();
            output = realSig;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return output;
    }

    //根据公钥对签名进行验证
    public static boolean verifyECDSASig(PublicKey publicKey, String data, byte[] signature) {
        try {
            Signature ecdsaVerify = Signature.getInstance("ECDSA", "BC");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(data.getBytes());
            return ecdsaVerify.verify(signature);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //把公私钥转成base64编码的字符串
    public static String getStringFromKey(Key key) {
        return Base64.getEncoder().encodeToString(key.getEncoded());
    }

    public static PublicKey getPublicKeyFromString(String key) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("ECDSA");

        X509EncodedKeySpec priPKCS8 = new X509EncodedKeySpec(

                Base64.getDecoder().decode(key));

        PublicKey publicKey = keyFactory.generatePublic(priPKCS8);
        return publicKey;
    }

    //关于默克尔树的很详细的描述： https://blog.csdn.net/wo541075754/article/details/54632929
    public static String getMerkleRoot(ArrayList<Transaction> transactions) {
        int count = transactions.size();
        ArrayList<String> previousTreeLayer = new ArrayList<String>();
        for (Transaction transaction : transactions) {
            previousTreeLayer.add(transaction.transactionId);
        }
        ArrayList<String> treeLayer = previousTreeLayer;
        while (count > 1) {
            treeLayer = new ArrayList<>();
            for (int i = 1; i < previousTreeLayer.size(); i++) {
                treeLayer.add(applySha256(previousTreeLayer.get(i - 1) + previousTreeLayer.get(i)));
            }
            count = treeLayer.size();
            previousTreeLayer = treeLayer;
        }
        String merkleRoot = (treeLayer.size() == 1) ? treeLayer.get(0) : "";
        return merkleRoot;
    }


    public static class ItemSerializer extends StdSerializer<Key> {

        public ItemSerializer() {
            this(null);
        }

        public ItemSerializer(Class<Key> t) {
            super(t);
        }

        @Override
        public void serialize(
                Key value, JsonGenerator jgen, SerializerProvider provider)
                throws IOException {
            //把publicKey和privateKey当作字符串来处理
            jgen.writeString(Utils.getStringFromKey(value));
        }
    }

    public static boolean isSameKey(PublicKey k1, PublicKey k2) {
        return getStringFromKey(k1).equals(getStringFromKey(k2));
    }
}
