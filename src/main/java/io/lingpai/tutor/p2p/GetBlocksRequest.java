package io.lingpai.tutor.p2p;

import io.netty.util.CharsetUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class GetBlocksRequest implements IMessage {
    //获取此区块之后的所有区块
    private String startHash; //开始区块的哈希

    public GetBlocksRequest(String startHash) {
        this.startHash = startHash;
    }

    public GetBlocksRequest(byte[] body) {
        this.startHash = new String(body,CharsetUtil.UTF_8);
    }

    @Override
    public byte getType() {
        return IMessage.GET_BLOCKS;
    }

    @Override
    public byte[] getBody() {
        return startHash.getBytes(CharsetUtil.UTF_8);
    }
}
