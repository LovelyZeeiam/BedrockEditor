package xueli.bedrockeditor;

import com.flowpowered.nbt.CompoundMap;
import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.IntTag;
import com.flowpowered.nbt.StringTag;
import com.flowpowered.nbt.stream.NBTOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.ArrayList;

/**
 * 子区块对象
 */
public class SubChunk {

    // 子区块中存储的Storage
    private ArrayList<Storage> storages = new ArrayList<>();

    public SubChunk(ArrayList<Storage> storages) {
        this.storages = storages;
    }

    public SubChunk() {
        storages.add(new Storage());

    }

    /**
     * @return 游戏中，目前观测是除了含水方块的存储，其它的方块都是在第一个Storage里面完成的，所以就写一个得到最主要的Storage的方法
     */
    public Storage getMainStorage() {
        return storages.get(0);
    }

    public Storage createNewStorage() {
        storages.add(storages.size(), new Storage());
        return storages.get(storages.size() - 1);
    }

    /**
     * 得到保存的东西
     * @return 需要保存在数据库里面的字节数据
     */
    byte[] getSaveData() {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            out.write(8);
            out.write(storages.size());

            for (Storage storage : storages) {
                ArrayList<CompoundTag> tags = storage.getTags();
                int[][][] indices = storage.getIndices();

                int bitPerBlock = getBitCount(tags.size() - 1);
                int blockPerWord = 32 / bitPerBlock;
                int paletteAndFlag = bitPerBlock << 1;
                out.write(paletteAndFlag);

                DataOutputStream outData = new DataOutputStream(out);
                for (int position = 0; position < 4096; ) {
                    int word = 0;
                    int[] data = new int[blockPerWord];
                    for (int m = 0; m < blockPerWord; m++) {
                        if (position >= 4096) {
                            break;
                        }

                        int x = (position >> 8) & 0xF;
                        int y = position & 0xF;
                        int z = (position >> 4) & 0xF;

                        data[m] = indices[x][y][z];

                        position++;

                    }

                    word = getWord(data, blockPerWord);
                    outData.writeInt(Integer.reverseBytes(word));
                }

                outData.writeInt(Integer.reverseBytes(tags.size()));

                NBTOutputStream nbtout = new NBTOutputStream(out, false, ByteOrder.LITTLE_ENDIAN);
                for (CompoundTag tag : tags)
                    nbtout.writeTag(tag);

            }

            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private int getWord(int[] data, int length) {
        if (length > 32) {
            System.err.println("Hey dude do u have any integer whose count of bits is more than 32?");
            return 0;
        }
        int bitsPerBlock = (int) (32.0 / length);

        int word = 0;

        for (int i = 0; i < data.length; i++) {
            int stateID = data[i];
            word |= (stateID << (i * bitsPerBlock));
        }

        return word;
    }

    private int getBitCount(int n) {
        return Integer.toBinaryString(n).length();
    }

    /**
     * 这个类是关于一个Storage对象。
     * <br/>
     * 主要思路就是体现了压缩的思想。That is to say, 将所有存在的不同的方块标签单独放在一个列表里面，将每个方块的存储简化成存储方块在前面那个列表对应的index，可以大大节省存储空间和内存
     */
    public static class Storage {
        private final int[][][] indices;
        private final ArrayList<CompoundTag> tags;

        public Storage() {
            this.tags = new ArrayList<>();
            this.indices = new int[16][16][16];

            // air block
            CompoundMap map = new CompoundMap();
            map.put(new IntTag("version", 17825808));
            map.put(new StringTag("name", "minecraft:air"));
            map.put(new CompoundTag("states", new CompoundMap()));
            CompoundTag airBlock = new CompoundTag("", map);
            this.tags.add(airBlock);

        }

        public Storage(int[][][] indices, ArrayList<CompoundTag> tags) {
            this.indices = indices;
            this.tags = tags;
        }

        public int[][][] getIndices() {
            return indices;
        }

        public ArrayList<CompoundTag> getTags() {
            return tags;
        }

    }

}
