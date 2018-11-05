package io.lingpai.tutor;

public class TransactionInput {
    private String transactionOutputId; //某个TransactionOutput的ID
    private TransactionOutput UTXO; //如果outputId对应的output已经被花费，则这个字段是null

    public TransactionInput(TransactionOutput output) {
        this.transactionOutputId = output.id;
        UTXO = output;
    }

    public String getTransactionOutputId() {
        return transactionOutputId;
    }

    public TransactionOutput getUTXO() {
        return UTXO;
    }

}
