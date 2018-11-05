package io.lingpai.tutor;


import java.security.PublicKey;

public class TransactionOutput {

    public String id;
    public PublicKey recipient; //接收方的公钥
    public float value; //
    public String parentTransactionId; //交易的id，当前这个output是该笔交易的一个output

    public TransactionOutput(PublicKey recipient, float value, String parentTransactionId) {
        this.recipient = recipient;
        this.value = value;
        this.parentTransactionId = parentTransactionId;
        this.id = Utils.applySha256(Utils.getStringFromKey(recipient)+Float.toString(value)+parentTransactionId);
    }

}
