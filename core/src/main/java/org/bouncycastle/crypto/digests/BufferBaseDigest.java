package org.bouncycastle.crypto.digests;

import org.bouncycastle.crypto.DataLengthException;
import org.bouncycastle.crypto.ExtendedDigest;
import org.bouncycastle.crypto.OutputLengthException;

public abstract class BufferBaseDigest
    implements ExtendedDigest
{
    protected enum ProcessingBufferType
    {
        Buffered,
        Immediate,
    }

    protected int DigestSize;
    protected int BlockSize;
    protected byte[] m_buf;
    protected int m_bufPos;
    protected String algorithmName;
    protected ProcessingBuffer processor;

    protected BufferBaseDigest(ProcessingBufferType type, int BlockSize)
    {
        this.BlockSize = BlockSize;
        m_buf = new byte[BlockSize];
        switch (type)
        {
        case Buffered:
            processor = new BufferedProcessor();
            break;
        case Immediate:
            processor = new ImmediateProcessor();
            break;
        }
    }

    protected interface ProcessingBuffer
    {
        void update(byte input);

        boolean isLengthWithinAvailableSpace(int len, int available);

        boolean isLengthExceedingBlockSize(int len, int size);
    }

    private class BufferedProcessor
        implements ProcessingBuffer
    {
        public void update(byte input)
        {
            if (m_bufPos == BlockSize)
            {
                processBytes(m_buf, 0);
                m_bufPos = 0;
            }
            m_buf[m_bufPos++] = input;
        }

        @Override
        public boolean isLengthWithinAvailableSpace(int len, int available)
        {
            return len <= available;
        }

        @Override
        public boolean isLengthExceedingBlockSize(int len, int size)
        {
            return len > size;
        }
    }

    private class ImmediateProcessor
        implements ProcessingBuffer
    {
        public void update(byte input)
        {
            m_buf[m_bufPos] = input;
            if (++m_bufPos == BlockSize)
            {
                processBytes(m_buf, 0);
                m_bufPos = 0;
            }
        }

        @Override
        public boolean isLengthWithinAvailableSpace(int len, int available)
        {
            return len < available;
        }

        @Override
        public boolean isLengthExceedingBlockSize(int len, int size)
        {
            return len >= size;
        }
    }

    @Override
    public String getAlgorithmName()
    {
        return algorithmName;
    }

    @Override
    public int getDigestSize()
    {
        return DigestSize;
    }

    @Override
    public int getByteLength()
    {
        return BlockSize;
    }

    @Override
    public void update(byte in)
    {
        processor.update(in);
    }

    @Override
    public void update(byte[] input, int inOff, int len)
    {
        if ((inOff + len) > input.length)
        {
            throw new DataLengthException("input buffer too short");
        }
        int available = BlockSize - m_bufPos;
        if (processor.isLengthWithinAvailableSpace(len, available))
        {
            System.arraycopy(input, inOff, m_buf, m_bufPos, len);
            m_bufPos += len;
            return;
        }
        if (m_bufPos > 0)
        {
            System.arraycopy(input, inOff, m_buf, m_bufPos, available);
            inOff += available;
            len -= available;
            processBytes(m_buf, 0);
        }
        while (processor.isLengthExceedingBlockSize(len, BlockSize))
        {
            processBytes(input, inOff);
            inOff += BlockSize;
            len -= BlockSize;
        }
        System.arraycopy(input, inOff, m_buf, 0, len);
        m_bufPos = len;
    }

    @Override
    public int doFinal(byte[] output, int outOff)
    {
        if (DigestSize + outOff > output.length)
        {
            throw new OutputLengthException("output buffer is too short");
        }
        finish(output, outOff);
        reset();
        return DigestSize;
    }

    protected abstract void processBytes(byte[] input, int inOff);

    protected abstract void finish(byte[] output, int outOff);
}
