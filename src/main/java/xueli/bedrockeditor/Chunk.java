package xueli.bedrockeditor;

import com.flowpowered.nbt.CompoundTag;

import java.util.HashMap;

/**
 * ������֪��Minecraft��������Ϊ�洢�����ݽ����Ļ�����λ�ģ����Ұ��ֽ�һ�������Ϊ8��С���飬ÿ��С����ĸ߶���16���������϶ѵ���
 * @see SubChunk
 * @author XueLi
 */
public class Chunk {

    private final SubChunk[] subChunks;
    private final int x;
    private final int z;
    private final int dimension;
    private HashMap<BlockPos, CompoundTag> blockEntities = new HashMap<>();

    public Chunk(SubChunk[] subChunks, HashMap<BlockPos, CompoundTag> blockEntities, int x, int z, int dimension) {
        this.subChunks = subChunks;
        this.x = x;
        this.z = z;
        this.dimension = dimension;
        this.blockEntities = blockEntities;

    }

    public Chunk(SubChunk[] subChunks, int x, int z, int dimension) {
        this.subChunks = subChunks;
        this.x = x;
        this.z = z;
        this.dimension = dimension;

    }

    public Chunk(int x, int z, int dimension) {
        this.subChunks = new SubChunk[16];
        this.x = x;
        this.z = z;
        this.dimension = dimension;

    }

    public HashMap<BlockPos, CompoundTag> getBlockEntities() {
        return blockEntities;
    }

    public SubChunk getSubChunk(int i) {
        if (subChunks[i] == null)
            return createNewSubchunk(i);
        return subChunks[i];
    }

    public SubChunk createNewSubchunk(int i) {
        subChunks[i] = new SubChunk();
        return subChunks[i];
    }

    SubChunk getSubChunkWhenSaved(int i) {
        return subChunks[i];
    }

    public int getX() {
        return x;
    }

    public int getZ() {
        return z;
    }

    public int getDimension() {
        return dimension;
    }

    public CompoundTag getBlock(int x, int y, int z) {
        SubChunk sc = subChunks[y >> 4];
        int yoffset = y - ((y >> 4) << 4);

        int i = sc.getMainStorage().getIndices()[x][yoffset][z];
        return sc.getMainStorage().getTags().get(i);
    }

    public void setBlock(int x, int y, int z, CompoundTag block) {
        SubChunk sc = subChunks[y >> 4];
        int yoffset = y - ((y >> 4) << 4);

        if (sc == null)
            sc = createNewSubchunk(y >> 4);

        SubChunk.Storage mainStorage = sc.getMainStorage();

        if (mainStorage.getTags().contains(block))
            mainStorage.getIndices()[x][yoffset][z] = mainStorage.getTags().indexOf(block);
        else {
            mainStorage.getIndices()[x][yoffset][z] = mainStorage.getTags().size();
            mainStorage.getTags().add(block);

        }

    }

    public boolean isEmpty() {
        for (int i = 0; i < 8; i++)
            if (subChunks[i] != null) return false;
        return true;
    }

}
