package io.lingpai.tutor.p2p;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.lingpai.tutor.Transaction;
import io.lingpai.tutor.Utils;
import io.netty.util.CharsetUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class TransactionMessage implements IMessage {
    private Transaction transaction;


    public TransactionMessage(Transaction transaction) {
        this.transaction = transaction;
    }

    public TransactionMessage(String body) {
        try {
            this.transaction = Utils.fromJson(body, Transaction.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte getType() {
        return NEW_TRANSACTION;
    }

    @Override
    public byte[] getBody() {
        try {
            return Utils.toJson(transaction).getBytes(CharsetUtil.UTF_8);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
