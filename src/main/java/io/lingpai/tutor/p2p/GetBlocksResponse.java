package io.lingpai.tutor.p2p;

import io.lingpai.tutor.Block;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class GetBlocksResponse implements IMessage {
    List<Block> blockList;

    public GetBlocksResponse(List<Block> blockList) {
        this.blockList = blockList;
    }

    public GetBlocksResponse(String body) {

    }

    @Override
    public byte getType() {
        return RETURN_BLOCKS;
    }

    @Override
    public byte[] getBody() {
        return new byte[0];
    }
}
