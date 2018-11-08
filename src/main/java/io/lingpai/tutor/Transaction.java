package io.lingpai.tutor;


import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;

@Slf4j
@NoArgsConstructor
public class Transaction {

    public String transactionId; // 交易的id, 也就是交易的哈希
    public PublicKey sender; // 发送方的公钥
    public PublicKey recipient; // 接收方的公钥
    public float value;
    public byte[] signature; // 签名，1:说明该笔交易肯定是发送方发出的，其他人无法修改

    private ArrayList<TransactionInput> inputs;
    //正常的交易至少2个output，一个是给接收方的，一个是给原地址的找零，
    private ArrayList<TransactionOutput> outputs = new ArrayList<>();

    private static int sequence = 0; // //每次计算哈希前都需要递增，保证同样金额的交易也不会得到同样的哈希

    //output可以根据input和value内部构造
    public  Transaction(PublicKey from, PublicKey to, float value,  ArrayList<TransactionInput> inputs) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        this.inputs = inputs;
    }

    public Transaction(PublicKey from, PublicKey to, float value) {
        this.sender = from;
        this.recipient = to;
        this.value = value;
        //没有input，说明该笔交易是系统奖励给挖矿钱包的
        this.inputs = new ArrayList<>();

        transactionId = calulateHash();
    }


    private String calulateHash() {
        sequence++;
        return Utils.applySha256(
                Utils.getStringFromKey(sender) +
                        Utils.getStringFromKey(recipient) +
                        Float.toString(value) + sequence
        );
    }

    //发送者对交易签名
    public void generateSignature(PrivateKey privateKey) {
        String data = Utils.getStringFromKey(sender) + Utils.getStringFromKey(recipient) + Float.toString(value)	;
        signature = Utils.applyECDSASig(privateKey,data);
    }

    //挖矿者对交易进行验证
    public boolean verifySignature() {
        String data = Utils.getStringFromKey(sender) + Utils.getStringFromKey(recipient) + Float.toString(value)	;
        return Utils.verifyECDSASig(sender, data, signature);
    }


    public boolean processTransaction() {

        if(verifySignature() == false) {
            System.out.println("#Transaction Signature failed to verify");
            return false;
        }

        //不是挖矿奖励的交易，就需要验证输入
        if (!sender.equals(CoconutChain.coinbase.publicKey)) {


            float leftOver = getInputsValue() - value; //get value of inputs then the left over change:
            if (leftOver > 0.0f) {
                //给原地址找零
                outputs.add(new TransactionOutput( this.sender, leftOver,transactionId));
            }

        }

        //generate transaction outputs:

        outputs.add(new TransactionOutput( this.recipient, value,transactionId)); //send value to recipient

        return true;
    }

    //returns sum of inputs(UTXOs) values
    public float getInputsValue() {
        float total = 0;
        for(TransactionInput i : inputs) {
            total += i.getUTXO().value;
        }
        return total;
    }

    //returns sum of outputs:
    public float getOutputsValue() {
        float total = 0;
        for(TransactionOutput o : outputs) {
            total += o.value;
        }
        return total;
    }

    public ArrayList<TransactionInput> getInputs() {
        return inputs;
    }



    public ArrayList<TransactionOutput> getOutputs() {
        return outputs;
    }

    public String getTransactionId() {
        return transactionId;
    }

}
