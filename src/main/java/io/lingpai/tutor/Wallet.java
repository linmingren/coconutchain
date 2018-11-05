package io.lingpai.tutor;

import lombok.extern.slf4j.Slf4j;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.util.ArrayList;
import java.util.Map;

@Slf4j
public class Wallet {
    public PrivateKey privateKey;
    public PublicKey publicKey;


    public Wallet() {
        generateKeyPair();
    }


    private void generateKeyPair() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("ECDSA", "BC");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
            ECGenParameterSpec ecSpec = new ECGenParameterSpec("prime192v1");
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(ecSpec, random);   //256 bytes provides an acceptable security level
            KeyPair keyPair = keyGen.generateKeyPair();
            // Set the public and private keys from the keyPair
            privateKey = keyPair.getPrivate();
            publicKey = keyPair.getPublic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public float getBalance() {
        float total = 0;
        for (Map.Entry<String, TransactionOutput> item : CoconutChain.UTXOs.entrySet()) {
            TransactionOutput UTXO = item.getValue();
            if (isMine(UTXO)) {
                total += UTXO.value;
            }
        }
        return total;
    }

    //Generates and returns a new transaction from this wallet.
    public Transaction sendFunds(PublicKey _recipient, float value) {
        if (getBalance() < value) { //gather balance and check funds.
            log.error("#Not Enough funds to send transaction. Transaction Discarded.");
            return null;
        }

        ArrayList<TransactionInput> inputs = new ArrayList<>();

        //找到足够多的input，让它的金额比value大，这样才能给对方转帐
        float total = 0;
        for (Map.Entry<String, TransactionOutput> item : CoconutChain.UTXOs.entrySet()) {

            TransactionOutput UTXO = item.getValue();
            if (isMine(UTXO)) {
                total += UTXO.value;
                inputs.add(new TransactionInput(UTXO));
                if (total >= value) break;
            }
        }

        Transaction newTransaction = new Transaction(publicKey, _recipient, value, inputs);
        newTransaction.generateSignature(privateKey);

        for (TransactionInput input : inputs) {
            CoconutChain.UTXOs.remove(input.getTransactionOutputId());
        }
        return newTransaction;
    }

    private boolean isMine(TransactionOutput output) {
        return output.recipient == publicKey;
    }
}
