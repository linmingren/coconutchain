package io.lingpai.tutor.p2p;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.lingpai.tutor.Block;
import io.lingpai.tutor.Utils;
import io.netty.util.CharsetUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class BlockMessage implements IMessage {
    private Block block;

    public BlockMessage(Block block) {
        this.block = block;
    }

    public BlockMessage(String body) {
        try {
            this.block = Utils.fromJson(body,Block.class);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public byte getType() {
        return NEW_BLOCK;
    }

    @Override
    public byte[] getBody() {
        try {
            return Utils.toJson(block).getBytes(CharsetUtil.UTF_8);
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
