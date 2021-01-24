package xueli.bedrockeditor;

import com.flowpowered.nbt.CompoundTag;
import com.flowpowered.nbt.stream.NBTInputStream;
import com.flowpowered.nbt.stream.NBTOutputStream;
import org.iq80.leveldb.CompressionType;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.WriteBatch;
import org.iq80.leveldb.impl.Iq80DBFactory;
import xueli.utils.Int2HashMap;
import xueli.utils.buffer.ByteBufferInputStream;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * 这是存档中db文件夹中存储的leveldb数据库的封装类，便于读取存档
 */
public class LevelDatabase {

    // leveldb读取的参数
    private static final Options options = new Options();
    private static final Iq80DBFactory factory = new Iq80DBFactory();

    static {
        options.createIfMissing(false);
        options.compressionType(CompressionType.SNAPPY);

    }

    // leveldb对象
    private final DB db;

    // 已经读取过的区块
    private final HashMap<Integer, Int2HashMap<Chunk>> chunks = new HashMap<>();

    /**
     *
     * @param path 存档中db目录的路径
     * @throws IOException 当读取出现错误时，抛出这个异常
     */
    public LevelDatabase(String path) throws IOException {
        db = factory.open(new File(path), options);

        chunks.put(0, new Int2HashMap<>());
        chunks.put(1, new Int2HashMap<>());
        chunks.put(2, new Int2HashMap<>());

    }

    /**
     * 获得一个区块：如果数据库中存在这个区块，就返回这个区块；如果不存在，创建一个新的区块；最后都将得到的区块放入上面的Hashmap chunks对象里面
     * @param chunkX 区块的x坐标
     * @param chunkZ 区块的z坐标
     * @param dimension 区块的维度，0代表主世界，1代表地狱，2代表下界
     * @return 读取到的区块
     * @throws IOException 当数据库读取出现错误时，抛出这个异常
     */
    public Chunk getChunk(int chunkX, int chunkZ, int dimension) throws IOException {
        // 如果发现已经访问过这个区块，就将存在缓存中的区块返回
        if (chunks.get(dimension).containsKey(chunkX, chunkZ))
            return chunks.get(dimension).get(chunkX, chunkZ);

        SubChunk[] subChunks = new SubChunk[16];

        // 获得所有的subchunk的存储数据
        byte[][] chunkData = new byte[16][];
        // 这个布尔值代表在这个区块内，一个子区块都没有搜索到
        boolean nullData = true;

        if (dimension == 0)
            for (byte i = 0; i < 16; i++) {
                chunkData[i] = db.get(new byte[]{(byte) (chunkX & 0xff), (byte) ((chunkX >> 8) & 0xff),
                        (byte) ((chunkX >> 16) & 0xff), (byte) ((chunkX >> 24) & 0xff), (byte) (chunkZ & 0xff),
                        (byte) ((chunkZ >> 8) & 0xff), (byte) ((chunkZ >> 16) & 0xff), (byte) ((chunkZ >> 24) & 0xff),
                        47, i});

                if (chunkData[i] != null) {
                    // Logger.print(Arrays.toString(chunkData[i]));
                    nullData = false;
                }

            }
        else
            for (byte i = 0; i < 16; i++) {
                chunkData[i] = db.get(new byte[]{(byte) (chunkX & 0xff), (byte) ((chunkX >> 8) & 0xff),
                        (byte) ((chunkX >> 16) & 0xff), (byte) ((chunkX >> 24) & 0xff), (byte) (chunkZ & 0xff),
                        (byte) ((chunkZ >> 8) & 0xff), (byte) ((chunkZ >> 16) & 0xff), (byte) ((chunkZ >> 24) & 0xff),
                        (byte) (dimension & 0xff), (byte) ((dimension >> 8) & 0xff), (byte) ((dimension >> 16) & 0xff),
                        (byte) ((dimension >> 24) & 0xff), 47, i});
                if (chunkData[i] != null) {
                    nullData = false;
                }
            }

            //如果真没有搜索到，就创建一个新的Chunk返回
        if (nullData) {
            Chunk chunk = new Chunk(chunkX, chunkZ, dimension);
            chunks.get(dimension).put(chunkX, chunkZ, chunk);
            return chunk;
        }

        for (int x = 0; x < 16; x++) {
            byte[] data = chunkData[x];
            if (data == null) continue;
            ArrayList<SubChunk.Storage> storages = processIn(ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN));
            subChunks[x] = new SubChunk(storages);

            //Files.fileOutput("game_" + x, data);

        }

        // 方块实体的读取
        HashMap<BlockPos, CompoundTag> blockEntities = new HashMap<>();

        {
            byte[] key;
            if (dimension == 0) {
                key = new byte[]{(byte) (chunkX & 0xff), (byte) ((chunkX >> 8) & 0xff),
                        (byte) ((chunkX >> 16) & 0xff), (byte) ((chunkX >> 24) & 0xff), (byte) (chunkZ & 0xff),
                        (byte) ((chunkZ >> 8) & 0xff), (byte) ((chunkZ >> 16) & 0xff), (byte) ((chunkZ >> 24) & 0xff),
                        49};
            } else {
                key = new byte[]{(byte) (chunkX & 0xff), (byte) ((chunkX >> 8) & 0xff), (byte) ((chunkX >> 16) & 0xff),
                        (byte) ((chunkX >> 24) & 0xff), (byte) (chunkZ & 0xff), (byte) ((chunkZ >> 8) & 0xff),
                        (byte) ((chunkZ >> 16) & 0xff), (byte) ((chunkZ >> 24) & 0xff),
                        (byte) (dimension & 0xff), (byte) ((dimension >> 8) & 0xff), (byte) ((dimension >> 16) & 0xff),
                        (byte) ((dimension >> 24) & 0xff), 49};
            }

            byte[] value = db.get(key);
            if (value != null) {
                NBTInputStream nbtin = new NBTInputStream(new ByteArrayInputStream(value), false, ByteOrder.LITTLE_ENDIAN);
                while (true) {
                    try {
                        CompoundTag tag = (CompoundTag) nbtin.readTag();

                        // 为方便修改，将方块对应的坐标存储到map的key
                        int x = (Integer) tag.getValue().get("x").getValue();
                        int y = (Integer) tag.getValue().get("y").getValue();
                        int z = (Integer) tag.getValue().get("z").getValue();

                        blockEntities.put(new BlockPos(x, y, z), tag);

                    } catch (EOFException e) {
                        // 发现读取到结尾了，就停止读取（硬核）
                        break;
                    }
                }

            }

        }

        Chunk chunk = new Chunk(subChunks, blockEntities, chunkX, chunkZ, dimension);
        chunks.get(dimension).put(chunkX, chunkZ, chunk);

        return chunk;
    }

    /**
     * 返回指定坐标的一个方块。如果发现这个坐标对应的区块不存在，会创建一个空的区块，并返回对应的空气方块的NBT。
     * @param x 方块的x坐标
     * @param y 方块的y坐标
     * @param z 方块的z坐标
     * @param dimension 方块所在的维度
     * @return 方块对应的方块NBT
     * @throws IOException 当读取区块的时候出现错误，会抛出这个异常
     */
    public CompoundTag getBlock(int x, int y, int z, int dimension) throws IOException {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        int xInChunk = x - (chunkX << 4);
        int zInChunk = z - (chunkZ << 4);

        Chunk chunk = getChunk(chunkX, chunkZ, dimension);
        return chunk.getBlock(xInChunk, y, zInChunk);
    }

    /**
     * 返回指定方块对应的方块实体NBT。如果发现这个坐标对应的方块实体不存在，就返回null
     * @param x 方块的x坐标
     * @param y 方块的y坐标
     * @param z 方块的z坐标
     * @param dimension 方块所在的维度
     * @return 方块对应的方块实体NBT
     * @throws IOException 当读取区块的时候出现错误，会抛出这个异常
     */
    public CompoundTag getBlockEntity(int x, int y, int z, int dimension) throws IOException {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        int xInChunk = x - (chunkX << 4);
        int zInChunk = z - (chunkZ << 4);

        Chunk chunk = getChunk(chunkX, chunkZ, dimension);
        return chunk.getBlockEntities().get(new BlockPos(xInChunk, y, zInChunk));
    }

    /**
     * 设置指定方块的方块实体nbt
     * @param x 方块的x坐标
     * @param y 方块的y坐标
     * @param z 方块的z坐标
     * @param dimension 方块所在的维度
     * @param blockEntity 方块实体NBT
     * @throws IOException 当读取区块的时候出现错误，会抛出这个异常
     */
    public void setBlockEntity(int x, int y, int z, int dimension, CompoundTag blockEntity) throws IOException {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        Chunk chunk = getChunk(chunkX, chunkZ, dimension);
        chunk.getBlockEntities().put(new BlockPos(x, y, z), blockEntity);

    }

    /**
     * 设置指定方块的方块nbt
     * @param x 方块的x坐标
     * @param y 方块的y坐标
     * @param z 方块的z坐标
     * @param dimension 方块所在的维度
     * @param blockTag 方块NBT
     * @throws IOException 当读取区块的时候出现错误，会抛出这个异常
     */
    public void setBlock(int x, int y, int z, int dimension, CompoundTag blockTag) throws IOException {
        int chunkX = x >> 4;
        int chunkZ = z >> 4;

        int xInChunk = x - (chunkX << 4);
        int zInChunk = z - (chunkZ << 4);

        Chunk chunk = getChunk(chunkX, chunkZ, dimension);
        chunk.setBlock(xInChunk, y, zInChunk, blockTag);

    }

    /**
     * 通过读取到的数据处理得到Chunk类。
     * <br/>
     * 这个地方的代码是在github上面的<a href="https://github.com/mjungnickel18/papyruscs>papyruscs</a>这个库得到的
     * @param in 将得到的数据变成ByteBuffer，so as to食用里面提供的类似DataInputStream中的read直接到某个基本类型的方法
     * @return 返回子区块存储的Storage (目前观测到只有含水方块会使用到第二个Storage)
     * @see SubChunk.Storage
     * @throws IOException 当读取leveldb发生错误，会抛出这个异常
     */
    public ArrayList<SubChunk.Storage> processIn(ByteBuffer in) throws IOException {
        // 版本号
        in.get();
        // storage
        byte storage = in.get();

        ArrayList<SubChunk.Storage> storages = new ArrayList<>();

        for (int i = 0; i < storage; i++) {
            byte flag = in.get();
            int bitsPerBlock = flag >> 1;

            boolean isRuntime = (flag & 1) != 0;

            int blocksPerWord = (int) Math.floor(32.0 / bitsPerBlock);
            int wordCount = (int) Math.ceil(4096.0 / blocksPerWord);

            int position = 0;
            int[][][] indices = new int[16][16][16];
            for (int wi = 0; wi < wordCount; wi++) {
                int word = in.getInt();

                for (int block = 0; block < blocksPerWord; block++) {
                    int state = (word >> ((position % blocksPerWord) * bitsPerBlock)) & ((1 << bitsPerBlock) - 1);

                    int x = (position >> 8) & 0xF;
                    int y = position & 0xF;
                    int z = (position >> 4) & 0xF;

                    indices[x][y][z] = state;

                    position++;

                }

            }

            ArrayList<CompoundTag> tags = new ArrayList<>();
            if (!isRuntime) {
                int paletteSize = in.getInt();
                for (int palletId = 0; palletId < paletteSize; palletId++) {
                    NBTInputStream nbtin = new NBTInputStream(new ByteBufferInputStream(in), false,
                            ByteOrder.LITTLE_ENDIAN);
                    CompoundTag tag = (CompoundTag) nbtin.readTag();
                    tags.add(tag);
                    nbtin.close();
                }
            }

            storages.add(new SubChunk.Storage(indices, tags));

        }

        return storages;
    }

    /**
     * 将区块保存回去。这个保存的方法是根据上面读取的方法自己写的
     * @throws IOException 当写区块的时候发生错误，就会抛出这个异常
     */
    public void close() throws IOException {
        WriteBatch batch = db.createWriteBatch();

        chunks.forEach((d, map) -> {
            map.forEach((n, chunk) -> {
                int x = chunk.getX();
                int z = chunk.getZ();
                int dimension = chunk.getDimension();

                // 区块格式版本
                {
                    byte[] key;
                    if (dimension == 0) {
                        key = new byte[]{(byte) (x & 0xff), (byte) ((x >> 8) & 0xff),
                                (byte) ((x >> 16) & 0xff), (byte) ((x >> 24) & 0xff), (byte) (z & 0xff),
                                (byte) ((z >> 8) & 0xff), (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                                118};
                    } else {
                        key = new byte[]{(byte) (x & 0xff), (byte) ((x >> 8) & 0xff), (byte) ((x >> 16) & 0xff),
                                (byte) ((x >> 24) & 0xff), (byte) (z & 0xff), (byte) ((z >> 8) & 0xff),
                                (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                                (byte) (dimension & 0xff), (byte) ((dimension >> 8) & 0xff), (byte) ((dimension >> 16) & 0xff),
                                (byte) ((dimension >> 24) & 0xff), 118};
                    }
                    batch.put(key, new byte[]{19});

                }

                // 方块实体
                {
                    byte[] key;
                    if (dimension == 0) {
                        key = new byte[]{(byte) (x & 0xff), (byte) ((x >> 8) & 0xff),
                                (byte) ((x >> 16) & 0xff), (byte) ((x >> 24) & 0xff), (byte) (z & 0xff),
                                (byte) ((z >> 8) & 0xff), (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                                49};
                    } else {
                        key = new byte[]{(byte) (x & 0xff), (byte) ((x >> 8) & 0xff), (byte) ((x >> 16) & 0xff),
                                (byte) ((x >> 24) & 0xff), (byte) (z & 0xff), (byte) ((z >> 8) & 0xff),
                                (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                                (byte) (dimension & 0xff), (byte) ((dimension >> 8) & 0xff), (byte) ((dimension >> 16) & 0xff),
                                (byte) ((dimension >> 24) & 0xff), 49};
                    }

                    final ByteArrayOutputStream out = new ByteArrayOutputStream();
                    try {
                        NBTOutputStream nbtout = new NBTOutputStream(out, false, ByteOrder.LITTLE_ENDIAN);
                        chunk.getBlockEntities().forEach((pos, tag) -> {
                            try {
                                nbtout.writeTag(tag);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    batch.put(key, out.toByteArray());

                }

                // 子区块的保存
                {
                    for (byte i = 0; i < 16; i++) {
                        SubChunk sc = chunk.getSubChunkWhenSaved(i);
                        if (sc == null)
                            continue;
                        byte[] value = sc.getSaveData();

                        byte[] key;
                        if (dimension == 0) {
                            key = new byte[]{(byte) (x & 0xff), (byte) ((x >> 8) & 0xff),
                                    (byte) ((x >> 16) & 0xff), (byte) ((x >> 24) & 0xff), (byte) (z & 0xff),
                                    (byte) ((z >> 8) & 0xff), (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                                    47, i};
                        } else {
                            key = new byte[]{(byte) (x & 0xff), (byte) ((x >> 8) & 0xff), (byte) ((x >> 16) & 0xff),
                                    (byte) ((x >> 24) & 0xff), (byte) (z & 0xff), (byte) ((z >> 8) & 0xff),
                                    (byte) ((z >> 16) & 0xff), (byte) ((z >> 24) & 0xff),
                                    (byte) (dimension & 0xff), (byte) ((dimension >> 8) & 0xff), (byte) ((dimension >> 16) & 0xff),
                                    (byte) ((dimension >> 24) & 0xff), 47, i};
                        }

                        db.delete(key);
                        batch.put(key, value);

                        /*try {
                            Files.fileOutput("code_" + i, value);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }*/

                    }
                }

            });
        });

        db.write(batch);
        db.close();

    }

}
