package io.lingpai.tutor.p2p;

public class MessageFactory {
    public static IMessage from(byte type, String body) {
        switch (type) {
            case IMessage.GET_BLOCKS:
                return new GetBlocksRequest(body);
            case IMessage.RETURN_BLOCKS:
                return new GetBlocksResponse(body);
            case IMessage.NEW_BLOCK:
                return new BlockMessage(body);
            case IMessage.NEW_TRANSACTION:
                return new TransactionMessage(body);
        }

        return null;
    }
}
