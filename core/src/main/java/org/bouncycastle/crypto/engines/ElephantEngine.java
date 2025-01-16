package org.bouncycastle.crypto.engines;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;

/**
 * Elephant AEAD v2, based on the current round 3 submission, https://www.esat.kuleuven.be/cosic/elephant/
 * Reference C implementation: https://github.com/TimBeyne/Elephant
 * Specification: https://csrc.nist.gov/CSRC/media/Projects/lightweight-cryptography/documents/finalist-round/updated-spec-doc/elephant-spec-final.pdf
 */
public class ElephantEngine
    extends AEADBufferBaseEngine
{
    public enum ElephantParameters
    {
        elephant160,
        elephant176,
        elephant200
    }

    private byte[] npub;
    private byte[] expanded_key;
    private int nb_its;
    private byte[] ad;
    private int adOff;
    private int adlen;
    private final byte[] tag_buffer;
    private byte[] previous_mask;
    private byte[] current_mask;
    private byte[] next_mask;
    private final byte[] buffer;
    private final byte[] previous_outputMessage;
    private final ByteArrayOutputStream aadData = new ByteArrayOutputStream();
    private int messageLen;
    private final Permutation instance;

    public ElephantEngine(ElephantParameters parameters)
    {
        super(ProcessingBufferType.Immediate);
        KEY_SIZE = 16;
        IV_SIZE = 12;
        switch (parameters)
        {
        case elephant160:
            BlockSize = 20;
            instance = new Dumbo();
            MAC_SIZE = 8;
            algorithmName = "Elephant 160 AEAD";
            break;
        case elephant176:
            BlockSize = 22;
            instance = new Jumbo();
            algorithmName = "Elephant 176 AEAD";
            MAC_SIZE = 8;
            break;
        case elephant200:
            BlockSize = 25;
            instance = new Delirium();
            algorithmName = "Elephant 200 AEAD";
            MAC_SIZE = 16;
            break;
        default:
            throw new IllegalArgumentException("Invalid parameter settings for Elephant");
        }
        m_bufferSizeDecrypt = BlockSize + MAC_SIZE;
        tag_buffer = new byte[BlockSize];
        previous_mask = new byte[BlockSize];
        current_mask = new byte[BlockSize];
        next_mask = new byte[BlockSize];
        buffer = new byte[BlockSize];
        m_buf = new byte[BlockSize + MAC_SIZE];
        previous_outputMessage = new byte[BlockSize];
        reset(false);
    }

    private interface Permutation
    {
        void permutation(byte[] state);

        void lfsr_step();
    }

    private abstract static class Spongent
        implements Permutation
    {
        private final byte lfsrIV;
        private final int nRounds;
        private final int nBits;
        private final int nSBox;
        private final byte[] sBoxLayer = {
            (byte)0xee, (byte)0xed, (byte)0xeb, (byte)0xe0, (byte)0xe2, (byte)0xe1, (byte)0xe4, (byte)0xef, (byte)0xe7, (byte)0xea, (byte)0xe8, (byte)0xe5, (byte)0xe9, (byte)0xec, (byte)0xe3, (byte)0xe6,
            (byte)0xde, (byte)0xdd, (byte)0xdb, (byte)0xd0, (byte)0xd2, (byte)0xd1, (byte)0xd4, (byte)0xdf, (byte)0xd7, (byte)0xda, (byte)0xd8, (byte)0xd5, (byte)0xd9, (byte)0xdc, (byte)0xd3, (byte)0xd6,
            (byte)0xbe, (byte)0xbd, (byte)0xbb, (byte)0xb0, (byte)0xb2, (byte)0xb1, (byte)0xb4, (byte)0xbf, (byte)0xb7, (byte)0xba, (byte)0xb8, (byte)0xb5, (byte)0xb9, (byte)0xbc, (byte)0xb3, (byte)0xb6,
            (byte)0x0e, (byte)0x0d, (byte)0x0b, (byte)0x00, (byte)0x02, (byte)0x01, (byte)0x04, (byte)0x0f, (byte)0x07, (byte)0x0a, (byte)0x08, (byte)0x05, (byte)0x09, (byte)0x0c, (byte)0x03, (byte)0x06,
            (byte)0x2e, (byte)0x2d, (byte)0x2b, (byte)0x20, (byte)0x22, (byte)0x21, (byte)0x24, (byte)0x2f, (byte)0x27, (byte)0x2a, (byte)0x28, (byte)0x25, (byte)0x29, (byte)0x2c, (byte)0x23, (byte)0x26,
            (byte)0x1e, (byte)0x1d, (byte)0x1b, (byte)0x10, (byte)0x12, (byte)0x11, (byte)0x14, (byte)0x1f, (byte)0x17, (byte)0x1a, (byte)0x18, (byte)0x15, (byte)0x19, (byte)0x1c, (byte)0x13, (byte)0x16,
            (byte)0x4e, (byte)0x4d, (byte)0x4b, (byte)0x40, (byte)0x42, (byte)0x41, (byte)0x44, (byte)0x4f, (byte)0x47, (byte)0x4a, (byte)0x48, (byte)0x45, (byte)0x49, (byte)0x4c, (byte)0x43, (byte)0x46,
            (byte)0xfe, (byte)0xfd, (byte)0xfb, (byte)0xf0, (byte)0xf2, (byte)0xf1, (byte)0xf4, (byte)0xff, (byte)0xf7, (byte)0xfa, (byte)0xf8, (byte)0xf5, (byte)0xf9, (byte)0xfc, (byte)0xf3, (byte)0xf6,
            (byte)0x7e, (byte)0x7d, (byte)0x7b, (byte)0x70, (byte)0x72, (byte)0x71, (byte)0x74, (byte)0x7f, (byte)0x77, (byte)0x7a, (byte)0x78, (byte)0x75, (byte)0x79, (byte)0x7c, (byte)0x73, (byte)0x76,
            (byte)0xae, (byte)0xad, (byte)0xab, (byte)0xa0, (byte)0xa2, (byte)0xa1, (byte)0xa4, (byte)0xaf, (byte)0xa7, (byte)0xaa, (byte)0xa8, (byte)0xa5, (byte)0xa9, (byte)0xac, (byte)0xa3, (byte)0xa6,
            (byte)0x8e, (byte)0x8d, (byte)0x8b, (byte)0x80, (byte)0x82, (byte)0x81, (byte)0x84, (byte)0x8f, (byte)0x87, (byte)0x8a, (byte)0x88, (byte)0x85, (byte)0x89, (byte)0x8c, (byte)0x83, (byte)0x86,
            (byte)0x5e, (byte)0x5d, (byte)0x5b, (byte)0x50, (byte)0x52, (byte)0x51, (byte)0x54, (byte)0x5f, (byte)0x57, (byte)0x5a, (byte)0x58, (byte)0x55, (byte)0x59, (byte)0x5c, (byte)0x53, (byte)0x56,
            (byte)0x9e, (byte)0x9d, (byte)0x9b, (byte)0x90, (byte)0x92, (byte)0x91, (byte)0x94, (byte)0x9f, (byte)0x97, (byte)0x9a, (byte)0x98, (byte)0x95, (byte)0x99, (byte)0x9c, (byte)0x93, (byte)0x96,
            (byte)0xce, (byte)0xcd, (byte)0xcb, (byte)0xc0, (byte)0xc2, (byte)0xc1, (byte)0xc4, (byte)0xcf, (byte)0xc7, (byte)0xca, (byte)0xc8, (byte)0xc5, (byte)0xc9, (byte)0xcc, (byte)0xc3, (byte)0xc6,
            (byte)0x3e, (byte)0x3d, (byte)0x3b, (byte)0x30, (byte)0x32, (byte)0x31, (byte)0x34, (byte)0x3f, (byte)0x37, (byte)0x3a, (byte)0x38, (byte)0x35, (byte)0x39, (byte)0x3c, (byte)0x33, (byte)0x36,
            (byte)0x6e, (byte)0x6d, (byte)0x6b, (byte)0x60, (byte)0x62, (byte)0x61, (byte)0x64, (byte)0x6f, (byte)0x67, (byte)0x6a, (byte)0x68, (byte)0x65, (byte)0x69, (byte)0x6c, (byte)0x63, (byte)0x66
        };

        public Spongent(int nBits, int nSBox, int nRounds, byte lfsrIV)
        {
            this.nRounds = nRounds;
            this.nSBox = nSBox;
            this.lfsrIV = lfsrIV;
            this.nBits = nBits;
        }

        public void permutation(byte[] state)
        {
            byte IV = lfsrIV;
            byte[] tmp = new byte[nSBox];
            for (int i = 0; i < nRounds; i++)
            {
                /* Add counter values */
                state[0] ^= IV;
                state[nSBox - 1] ^= (byte)(((IV & 0x01) << 7) | ((IV & 0x02) << 5) | ((IV & 0x04) << 3) | ((IV & 0x08)
                    << 1) | ((IV & 0x10) >>> 1) | ((IV & 0x20) >>> 3) | ((IV & 0x40) >>> 5) | ((IV & 0x80) >>> 7));
                IV = (byte)(((IV << 1) | (((0x40 & IV) >>> 6) ^ ((0x20 & IV) >>> 5))) & 0x7f);
                /* sBoxLayer layer */
                for (int j = 0; j < nSBox; j++)
                {
                    state[j] = sBoxLayer[(state[j] & 0xFF)];
                }
                /* pLayer */
                int PermutedBitNo;
                Arrays.fill(tmp, (byte)0);
                for (int j = 0; j < nSBox; j++)
                {
                    for (int k = 0; k < 8; k++)
                    {
                        PermutedBitNo = (j << 3) + k;
                        if (PermutedBitNo != nBits - 1)
                        {
                            PermutedBitNo = ((PermutedBitNo * nBits) >> 2) % (nBits - 1);
                        }
                        tmp[PermutedBitNo >>> 3] ^= (((state[j] & 0xFF) >>> k) & 0x1) << (PermutedBitNo & 7);
                    }
                }
                System.arraycopy(tmp, 0, state, 0, nSBox);
            }
        }
    }

    private class Dumbo
        extends Spongent
    {

        public Dumbo()
        {
            super(160, 20, 80, (byte)0x75);
        }

        @Override
        public void lfsr_step()
        {
            next_mask[BlockSize - 1] = (byte)((((current_mask[0] & 0xFF) << 3) | ((current_mask[0] & 0xFF) >>> 5)) ^
                ((current_mask[3] & 0xFF) << 7) ^ ((current_mask[13] & 0xFF) >>> 7));
        }
    }

    private class Jumbo
        extends Spongent
    {

        public Jumbo()
        {
            super(176, 22, 90, (byte)0x45);
        }

        @Override
        public void lfsr_step()
        {
            next_mask[BlockSize - 1] = (byte)(rotl(current_mask[0]) ^ ((current_mask[3] & 0xFF) << 7) ^ ((current_mask[19] & 0xFF) >>> 7));
        }
    }

    private class Delirium
        implements Permutation
    {
        private static final int nRounds = 18;
        private final byte[] KeccakRoundConstants = {
            (byte)0x01, (byte)0x82, (byte)0x8a, (byte)0x00, (byte)0x8b, (byte)0x01, (byte)0x81, (byte)0x09, (byte)0x8a,
            (byte)0x88, (byte)0x09, (byte)0x0a, (byte)0x8b, (byte)0x8b, (byte)0x89, (byte)0x03, (byte)0x02, (byte)0x80
        };

        private final int[] KeccakRhoOffsets = {0, 1, 6, 4, 3, 4, 4, 6, 7, 4, 3, 2, 3, 1, 7, 1, 5, 7, 5, 0, 2, 2, 5, 0, 6};

        @Override
        public void permutation(byte[] state)
        {
            for (int i = 0; i < nRounds; i++)
            {
                KeccakP200Round(state, i);
            }
        }

        @Override
        public void lfsr_step()
        {
            next_mask[BlockSize - 1] = (byte)(rotl(current_mask[0]) ^ rotl(current_mask[2]) ^ (current_mask[13] << 1));
        }

        private void KeccakP200Round(byte[] state, int indexRound)
        {
            int x, y;
            byte[] tempA = new byte[25];
            //theta
            for (x = 0; x < 5; x++)
            {
                for (y = 0; y < 5; y++)
                {
                    tempA[x] ^= state[index(x, y)];
                }
            }
            for (x = 0; x < 5; x++)
            {
                tempA[x + 5] = (byte)(ROL8(tempA[(x + 1) % 5], 1) ^ tempA[(x + 4) % 5]);
            }
            for (x = 0; x < 5; x++)
            {
                for (y = 0; y < 5; y++)
                {
                    state[index(x, y)] ^= tempA[x + 5];
                }
            }
            //rho
            for (x = 0; x < 5; x++)
            {
                for (y = 0; y < 5; y++)
                {
                    tempA[index(x, y)] = ROL8(state[index(x, y)], KeccakRhoOffsets[index(x, y)]);
                }
            }
            //pi
            for (x = 0; x < 5; x++)
            {
                for (y = 0; y < 5; y++)
                {
                    state[index(y, (2 * x + 3 * y) % 5)] = tempA[index(x, y)];
                }
            }
            //chi
            for (y = 0; y < 5; y++)
            {
                for (x = 0; x < 5; x++)
                {
                    tempA[x] = (byte)(state[index(x, y)] ^ ((~state[index((x + 1) % 5, y)]) & state[index((x + 2) % 5, y)]));
                }
                for (x = 0; x < 5; x++)
                {
                    state[index(x, y)] = tempA[x];
                }
            }
            //iota
            state[0] ^= KeccakRoundConstants[indexRound];//index(0,0)
        }

        private byte ROL8(byte a, int offset)
        {
            return (byte)((offset != 0) ? (((a & 0xFF) << offset) ^ ((a & 0xFF) >>> (8 - offset))) : a);
        }

        private int index(int x, int y)
        {
            return x + y * 5;
        }
    }

    private byte rotl(byte b)
    {
        return (byte)(((b & 0xFF) << 1) | ((b & 0xFF) >>> 7));
    }


    // State should be BLOCK_SIZE bytes long
    // Note: input may be equal to output
    private void lfsr_step()
    {
        instance.lfsr_step();
        System.arraycopy(current_mask, 1, next_mask, 0, BlockSize - 1);
    }

    private void xor_block(byte[] state, byte[] block, int bOff, int size)
    {
        for (int i = 0; i < size; ++i)
        {
            state[i] ^= block[i + bOff];
        }
    }

    @Override
    protected void init(byte[] k, byte[] iv)
        throws IllegalArgumentException
    {
        npub = iv;
        // Storage for the expanded key L
        expanded_key = new byte[BlockSize];
        System.arraycopy(k, 0, expanded_key, 0, KEY_SIZE);
        instance.permutation(expanded_key);
        m_state = forEncryption ? State.EncInit : State.DecInit;
        reset(false);
    }

    @Override
    public void processAADByte(byte input)
    {
        aadData.write(input);
    }

    @Override
    public void processAADBytes(byte[] input, int inOff, int len)
    {
        ensureSufficientInputBuffer(input, inOff, len);
        aadData.write(input, inOff, len);
    }

    protected void processBufferEncrypt(byte[] input, int inOff, byte[] output, int outOff)
    {
        processBuffer(input, inOff, output, outOff, State.EncData);
        System.arraycopy(output, outOff, previous_outputMessage, 0, BlockSize);
    }

    private void processBuffer(byte[] input, int inOff, byte[] output, int outOff, State encData)
    {
        if (m_state == State.DecInit || m_state == State.EncInit)
        {
            processAADBytes();
        }
        // Compute mask for the next message
        lfsr_step();

        // Compute ciphertext block
        computerCipherBlock(input, inOff, BlockSize, output, outOff);

        if (nb_its > 0)
        {
            // enough ciphertext
            System.arraycopy(previous_outputMessage, 0, buffer, 0, BlockSize);
            absorbCiphertext();
        }
        // If there is any AD left, compute tag for AD block
        if (m_state != encData)
        {
            absorbAAD();
        }
        // Cyclically shift the mask buffers
        // Value of next_mask will be computed in the next iteration
        swapMasks();
        nb_its++;
        messageLen += BlockSize;
    }

    protected void processBufferDecrypt(byte[] input, int inOff, byte[] output, int outOff)
    {
        processBuffer(input, inOff, output, outOff, State.DecData);
        System.arraycopy(input, inOff, previous_outputMessage, 0, BlockSize);
    }

    private void computerCipherBlock(byte[] input, int inOff, int blockSize, byte[] output, int outOff)
    {
        System.arraycopy(npub, 0, buffer, 0, IV_SIZE);
        Arrays.fill(buffer, IV_SIZE, BlockSize, (byte)0);
        xor_block(buffer, current_mask, 0, BlockSize);
        xor_block(buffer, next_mask, 0, BlockSize);
        instance.permutation(buffer);
        xor_block(buffer, current_mask, 0, BlockSize);
        xor_block(buffer, next_mask, 0, BlockSize);

        xor_block(buffer, input, inOff, blockSize);
        System.arraycopy(buffer, 0, output, outOff, blockSize);
    }

    private void swapMasks()
    {
        byte[] temp = previous_mask;
        previous_mask = current_mask;
        current_mask = next_mask;
        next_mask = temp;
    }

    private void absorbAAD()
    {
        processAADBytes(buffer);
        xor_block(buffer, next_mask, 0, BlockSize);
        instance.permutation(buffer);
        xor_block(buffer, next_mask, 0, BlockSize);
        xor_block(tag_buffer, buffer, 0, BlockSize);
    }

    private void absorbCiphertext()
    {
        xor_block(buffer, previous_mask, 0, BlockSize);
        xor_block(buffer, next_mask, 0, BlockSize);
        instance.permutation(buffer);
        xor_block(buffer, previous_mask, 0, BlockSize);
        xor_block(buffer, next_mask, 0, BlockSize);
        xor_block(tag_buffer, buffer, 0, BlockSize);
    }

    protected void processFinalBlock(byte[] output, int outOff)
    {
        int len = m_bufPos;
        int mlen = len + messageLen;
        processAADBytes();
        int nblocks_c = 1 + mlen / BlockSize;
        int nblocks_m = (mlen % BlockSize) != 0 ? nblocks_c : nblocks_c - 1;
        int nblocks_ad = 1 + (IV_SIZE + adlen) / BlockSize;
        int nb_it = Math.max(nblocks_c + 1, nblocks_ad - 1);
        processBytes(m_buf, output, outOff, nb_it, nblocks_m, nblocks_c, mlen, nblocks_ad);
        mac = new byte[MAC_SIZE];
        xor_block(tag_buffer, expanded_key, 0, BlockSize);
        instance.permutation(tag_buffer);
        xor_block(tag_buffer, expanded_key, 0, BlockSize);
        System.arraycopy(tag_buffer, 0, mac, 0, MAC_SIZE);
    }

    @Override
    protected void processBufferAAD(byte[] input, int inOff)
    {

    }

    @Override
    protected void processFinalAAD()
    {

    }

    @Override
    public int getUpdateOutputSize(int len)
    {
        switch (m_state)
        {
        case Uninitialized:
            throw new IllegalArgumentException(algorithmName + " needs call init function before getUpdateOutputSize");
        case DecFinal:
        case EncFinal:
            return 0;
        case EncAad:
        case EncData:
        case EncInit:
        {
            int total = m_bufPos + len;
            return total - total % BlockSize;
        }
        case DecAad:
        case DecData:
        case DecInit:
        {
            int total = Math.max(0, m_bufPos + len - MAC_SIZE);
            return total - total % BlockSize;
        }
        }
        return Math.max(0, len + m_bufPos - MAC_SIZE);
    }

    @Override
    public int getOutputSize(int len)
    {
        switch (m_state)
        {
        case Uninitialized:
            throw new IllegalArgumentException(algorithmName + " needs call init function before getUpdateOutputSize");
        case DecFinal:
        case EncFinal:
            return 0;
        case EncAad:
        case EncData:
        case EncInit:
            return len + m_bufPos + MAC_SIZE;
        }
        return Math.max(0, len + m_bufPos - MAC_SIZE);
    }

    private void processAADBytes()
    {
        if (adOff == -1)
        {
            ad = aadData.toByteArray();
            adOff = 0;
            adlen = ad.length;
            aadData.reset();
        }
        switch (m_state)
        {
        case EncInit:
        case DecInit:
            processAADBytes(tag_buffer);
            break;
        }
    }

    protected void reset(boolean clearMac)
    {
        aadData.reset();
        Arrays.fill(tag_buffer, (byte)0);
        Arrays.fill(previous_outputMessage, (byte)0);
        m_bufPos = 0;
        nb_its = 0;
        adOff = -1;
        messageLen = 0;
        super.reset(clearMac);
    }

    protected void checkAAD()
    {
        switch (m_state)
        {
        case DecData:
            throw new IllegalArgumentException(algorithmName + " cannot process AAD when the length of the plaintext to be processed exceeds the a block size");
        case EncData:
            throw new IllegalArgumentException(algorithmName + " cannot process AAD when the length of the ciphertext to be processed exceeds the a block size");
        case EncFinal:
            throw new IllegalArgumentException(algorithmName + " cannot be reused for encryption");
        default:
            break;
        }
    }

    protected boolean checkData()
    {
        switch (m_state)
        {
        case DecInit:
        case DecAad:
        case DecData:
            return false;
        case EncInit:
        case EncAad:
        case EncData:
            return true;
        case EncFinal:
            throw new IllegalStateException(getAlgorithmName() + " cannot be reused for encryption");
        default:
            throw new IllegalStateException(getAlgorithmName() + " needs to be initialized");
        }
    }

    private void processAADBytes(byte[] output)
    {
        int len = 0;
        switch (m_state)
        {
        case DecInit:
            System.arraycopy(expanded_key, 0, current_mask, 0, BlockSize);
            System.arraycopy(npub, 0, output, 0, IV_SIZE);
            len += IV_SIZE;
            m_state = State.DecAad;
            break;
        case EncInit:
            System.arraycopy(expanded_key, 0, current_mask, 0, BlockSize);
            System.arraycopy(npub, 0, output, 0, IV_SIZE);
            len += IV_SIZE;
            m_state = State.EncAad;
            break;
        case DecAad:
        case EncAad:
            // If adlen is divisible by BLOCK_SIZE, add an additional padding block
            if (adOff == adlen)
            {
                Arrays.fill(output, 0, BlockSize, (byte)0);
                output[0] = 0x01;
                return;
            }
            break;
        }
        int r_outlen = BlockSize - len;
        int r_adlen = adlen - adOff;
        // Fill with associated data if available
        if (r_outlen <= r_adlen)
        { // enough AD
            System.arraycopy(ad, adOff, output, len, r_outlen);
            adOff += r_outlen;
        }
        else
        { // not enough AD, need to pad
            if (r_adlen > 0) // ad might be nullptr
            {
                System.arraycopy(ad, adOff, output, len, r_adlen);
                adOff += r_adlen;
            }
            Arrays.fill(output, len + r_adlen, len + r_outlen, (byte)0);
            output[len + r_adlen] = 0x01;
            switch (m_state)
            {
            case DecAad:
                m_state = State.DecData;
                break;
            case EncAad:
                m_state = State.EncData;
                break;
            }
        }
    }

    private void processBytes(byte[] m, byte[] output, int outOff, int nb_it, int nblocks_m, int nblocks_c, int mlen,
                              int nblocks_ad)
    {
        int rv = 0;
        byte[] outputMessage = new byte[BlockSize];
        int i;
        for (i = nb_its; i < nb_it; ++i)
        {
            int r_size = (i == nblocks_m - 1) ? mlen - i * BlockSize : BlockSize;
            // Compute mask for the next message
            lfsr_step();
            if (i < nblocks_m)
            {
                // Compute ciphertext block
                computerCipherBlock(m, rv, r_size, output, outOff);
                if (forEncryption)
                {
                    System.arraycopy(buffer, 0, outputMessage, 0, r_size);
                }
                else
                {
                    System.arraycopy(m, rv, outputMessage, 0, r_size);
                }

                outOff += r_size;
                rv += r_size;
            }
            if (i > 0 && i <= nblocks_c)
            {
                //get_c_block: Compute tag for ciphertext block
                int block_offset = (i - 1) * BlockSize;
                // If clen is divisible by BLOCK_SIZE, add an additional padding block
                if (block_offset == mlen)
                {
                    Arrays.fill(buffer, 0, BlockSize, (byte)0);
                    buffer[0] = 0x01;
                }
                else
                {
                    int r_clen = mlen - block_offset;
                    // Fill with ciphertext if available
                    if (BlockSize <= r_clen)
                    { // enough ciphertext
                        System.arraycopy(previous_outputMessage, 0, buffer, 0, BlockSize);
                    }
                    else
                    { // not enough ciphertext, need to pad
                        if (r_clen > 0) // c might be nullptr
                        {
                            System.arraycopy(previous_outputMessage, 0, buffer, 0, r_clen);
                            Arrays.fill(buffer, r_clen, BlockSize, (byte)0);
                            buffer[r_clen] = 0x01;
                        }
                    }
                }

                absorbCiphertext();
            }
            // If there is any AD left, compute tag for AD block
            if (i + 1 < nblocks_ad)
            {
                absorbAAD();
            }
            // Cyclically shift the mask buffers
            // Value of next_mask will be computed in the next iteration
            swapMasks();
            System.arraycopy(outputMessage, 0, previous_outputMessage, 0, BlockSize);
        }
        nb_its = i;
    }
}
