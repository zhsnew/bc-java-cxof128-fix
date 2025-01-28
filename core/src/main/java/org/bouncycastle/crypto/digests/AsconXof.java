package org.bouncycastle.crypto.digests;

import org.bouncycastle.crypto.Xof;
import org.bouncycastle.util.Pack;

/**
 * ASCON v1.2 XOF, https://ascon.iaik.tugraz.at/ .
 * <p>
 * https://csrc.nist.gov/CSRC/media/Projects/lightweight-cryptography/documents/finalist-round/updated-spec-doc/ascon-spec-final.pdf
 * <p>
 * ASCON v1.2 XOF with reference to C Reference Impl from: https://github.com/ascon/ascon-c .
 *
 * @deprecated Now superseded - please use AsconXof128
 */
public class AsconXof
    extends AsconBaseDigest
    implements Xof
{
    public enum AsconParameters
    {
        AsconXof,
        AsconXofA,
    }

    AsconXof.AsconParameters asconParameters;

    public AsconXof(AsconXof.AsconParameters parameters)
    {
        BlockSize = 8;
        this.asconParameters = parameters;
        switch (parameters)
        {
        case AsconXof:
            ASCON_PB_ROUNDS = 12;
            algorithmName = "Ascon-Xof";
            break;
        case AsconXofA:
            ASCON_PB_ROUNDS = 8;
            algorithmName = "Ascon-XofA";
            break;
        default:
            throw new IllegalArgumentException("Invalid parameter settings for Ascon Hash");
        }
        reset();
    }

    private boolean m_squeezing = false;

    @Override
    public void update(byte in)
    {
        if (m_squeezing)
        {
            throw new IllegalArgumentException("attempt to absorb while squeezing");
        }
        super.update(in);
    }

    @Override
    public void update(byte[] input, int inOff, int len)
    {
        if (m_squeezing)
        {
            throw new IllegalArgumentException("attempt to absorb while squeezing");
        }
        super.update(input, inOff, len);
    }

    protected void padAndAbsorb()
    {
        m_squeezing = true;
        super.padAndAbsorb();
    }

    protected long pad(int i)
    {
        return 0x80L << (56 - (i << 3));
    }

    protected long loadBytes(final byte[] bytes, int inOff)
    {
        return Pack.bigEndianToLong(bytes, inOff);
    }

    protected long loadBytes(final byte[] bytes, int inOff, int n)
    {
        return Pack.bigEndianToLong(bytes, inOff, n);
    }

    protected void setBytes(long w, byte[] bytes, int inOff)
    {
        Pack.longToBigEndian(w, bytes, inOff);
    }

    protected void setBytes(long w, byte[] bytes, int inOff, int n)
    {
        Pack.longToBigEndian(w, bytes, inOff, n);
    }

    @Override
    public int doOutput(byte[] output, int outOff, int outLen)
    {
        return hash(output, outOff, outLen);
    }

    @Override
    public int doFinal(byte[] output, int outOff, int outLen)
    {
        int rlt = doOutput(output, outOff, outLen);
        reset();
        return rlt;
    }

    @Override
    public void reset()
    {
        super.reset();
        m_squeezing = false;
        /* initialize */
        switch (asconParameters)
        {
        case AsconXof:
            p.set(-5368810569253202922L, 3121280575360345120L, 7395939140700676632L, 6533890155656471820L, 5710016986865767350L);
            break;
        case AsconXofA:
            p.set(4940560291654768690L, -3635129828240960206L, -597534922722107095L, 2623493988082852443L, -6283826724160825537L);
            break;
        }
    }
}

