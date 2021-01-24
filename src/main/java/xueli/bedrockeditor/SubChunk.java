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
 * ���������
 */
public class SubChunk {

    // �������д洢��Storage
    private ArrayList<Storage> storages = new ArrayList<>();

    public SubChunk(ArrayList<Storage> storages) {
        this.storages = storages;
    }

    public SubChunk() {
        storages.add(new Storage());

    }

    /**
     * @return ��Ϸ�У�Ŀǰ�۲��ǳ��˺�ˮ����Ĵ洢�������ķ��鶼���ڵ�һ��Storage������ɵģ����Ծ�дһ���õ�����Ҫ��Storage�ķ���
     */
    public Storage getMainStorage() {
        return storages.get(0);
    }

    public Storage createNewStorage() {
        storages.add(storages.size(), new Storage());
        return storages.get(storages.size() - 1);
    }

    /**
     * �õ�����Ķ���
     * @return ��Ҫ���������ݿ�������ֽ�����
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
     * ������ǹ���һ��Storage����
     * <br/>
     * ��Ҫ˼·����������ѹ����˼�롣That is to say, �����д��ڵĲ�ͬ�ķ����ǩ��������һ���б����棬��ÿ������Ĵ洢�򻯳ɴ洢������ǰ���Ǹ��б��Ӧ��index�����Դ���ʡ�洢�ռ���ڴ�
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
