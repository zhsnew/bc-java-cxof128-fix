package org.bouncycastle.pqc.crypto.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.bouncycastle.asn1.ASN1BitString;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.ASN1ObjectIdentifier;
import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.ASN1Primitive;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.bc.BCObjectIdentifiers;
import org.bouncycastle.internal.asn1.isara.IsaraObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.pqc.asn1.CMCEPublicKey;
import org.bouncycastle.pqc.asn1.KyberPublicKey;
import org.bouncycastle.pqc.asn1.PQCObjectIdentifiers;
import org.bouncycastle.pqc.asn1.SPHINCS256KeyParams;
import org.bouncycastle.pqc.crypto.bike.BIKEParameters;
import org.bouncycastle.pqc.crypto.bike.BIKEPublicKeyParameters;
import org.bouncycastle.pqc.crypto.cmce.CMCEParameters;
import org.bouncycastle.pqc.crypto.cmce.CMCEPublicKeyParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumParameters;
import org.bouncycastle.pqc.crypto.crystals.dilithium.DilithiumPublicKeyParameters;
import org.bouncycastle.pqc.crypto.mlkem.KyberParameters;
import org.bouncycastle.pqc.crypto.mlkem.KyberPublicKeyParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconParameters;
import org.bouncycastle.pqc.crypto.falcon.FalconPublicKeyParameters;
import org.bouncycastle.pqc.crypto.frodo.FrodoParameters;
import org.bouncycastle.pqc.crypto.frodo.FrodoPublicKeyParameters;
import org.bouncycastle.pqc.crypto.hqc.HQCParameters;
import org.bouncycastle.pqc.crypto.hqc.HQCPublicKeyParameters;
import org.bouncycastle.pqc.crypto.newhope.NHPublicKeyParameters;
import org.bouncycastle.pqc.crypto.picnic.PicnicParameters;
import org.bouncycastle.pqc.crypto.picnic.PicnicPublicKeyParameters;
import org.bouncycastle.pqc.crypto.saber.SABERParameters;
import org.bouncycastle.pqc.crypto.saber.SABERPublicKeyParameters;
import org.bouncycastle.util.Arrays;
import org.bouncycastle.util.Pack;

/**
 * Factory to create asymmetric public key parameters for asymmetric ciphers from range of
 * ASN.1 encoded SubjectPublicKeyInfo objects.
 */
public class PublicKeyFactory
{
    private static Map converters = new HashMap();

    static
    {
        converters.put(PQCObjectIdentifiers.newHope, new NHConverter());


        
        converters.put(BCObjectIdentifiers.mceliece348864_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.mceliece348864f_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.mceliece460896_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.mceliece460896f_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.mceliece6688128_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.mceliece6688128f_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.mceliece6960119_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.mceliece6960119f_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.mceliece8192128_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.mceliece8192128f_r3, new CMCEConverter());
        converters.put(BCObjectIdentifiers.frodokem640aes, new FrodoConverter());
        converters.put(BCObjectIdentifiers.frodokem640shake, new FrodoConverter());
        converters.put(BCObjectIdentifiers.frodokem976aes, new FrodoConverter());
        converters.put(BCObjectIdentifiers.frodokem976shake, new FrodoConverter());
        converters.put(BCObjectIdentifiers.frodokem1344aes, new FrodoConverter());
        converters.put(BCObjectIdentifiers.frodokem1344shake, new FrodoConverter());
        converters.put(BCObjectIdentifiers.lightsaberkem128r3, new SABERConverter());
        converters.put(BCObjectIdentifiers.saberkem128r3, new SABERConverter());
        converters.put(BCObjectIdentifiers.firesaberkem128r3, new SABERConverter());
        converters.put(BCObjectIdentifiers.lightsaberkem192r3, new SABERConverter());
        converters.put(BCObjectIdentifiers.saberkem192r3, new SABERConverter());
        converters.put(BCObjectIdentifiers.firesaberkem192r3, new SABERConverter());
        converters.put(BCObjectIdentifiers.lightsaberkem256r3, new SABERConverter());
        converters.put(BCObjectIdentifiers.saberkem256r3, new SABERConverter());
        converters.put(BCObjectIdentifiers.firesaberkem256r3, new SABERConverter());
        converters.put(BCObjectIdentifiers.ulightsaberkemr3, new SABERConverter());
        converters.put(BCObjectIdentifiers.usaberkemr3, new SABERConverter());
        converters.put(BCObjectIdentifiers.ufiresaberkemr3, new SABERConverter());
        converters.put(BCObjectIdentifiers.lightsaberkem90sr3, new SABERConverter());
        converters.put(BCObjectIdentifiers.saberkem90sr3, new SABERConverter());
        converters.put(BCObjectIdentifiers.firesaberkem90sr3, new SABERConverter());
        converters.put(BCObjectIdentifiers.ulightsaberkem90sr3, new SABERConverter());
        converters.put(BCObjectIdentifiers.usaberkem90sr3, new SABERConverter());
        converters.put(BCObjectIdentifiers.ufiresaberkem90sr3, new SABERConverter());
        converters.put(BCObjectIdentifiers.picnicl1fs, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnicl1ur, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnicl3fs, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnicl3ur, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnicl5fs, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnicl5ur, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnic3l1, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnic3l3, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnic3l5, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnicl1full, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnicl3full, new PicnicConverter());
        converters.put(BCObjectIdentifiers.picnicl5full, new PicnicConverter());
        converters.put(BCObjectIdentifiers.falcon_512, new FalconConverter());
        converters.put(BCObjectIdentifiers.falcon_1024, new FalconConverter());
        converters.put(BCObjectIdentifiers.kyber512, new KyberConverter());
        converters.put(BCObjectIdentifiers.kyber768, new KyberConverter());
        converters.put(BCObjectIdentifiers.kyber1024, new KyberConverter());
        converters.put(BCObjectIdentifiers.kyber512_aes, new KyberConverter());
        converters.put(BCObjectIdentifiers.kyber768_aes, new KyberConverter());
        converters.put(BCObjectIdentifiers.kyber1024_aes, new KyberConverter());
        converters.put(BCObjectIdentifiers.dilithium2, new DilithiumConverter());
        converters.put(BCObjectIdentifiers.dilithium3, new DilithiumConverter());
        converters.put(BCObjectIdentifiers.dilithium5, new DilithiumConverter());
        converters.put(BCObjectIdentifiers.dilithium2_aes, new DilithiumConverter());
        converters.put(BCObjectIdentifiers.dilithium3_aes, new DilithiumConverter());
        converters.put(BCObjectIdentifiers.dilithium5_aes, new DilithiumConverter());
        converters.put(BCObjectIdentifiers.bike128, new BIKEConverter());
        converters.put(BCObjectIdentifiers.bike192, new BIKEConverter());
        converters.put(BCObjectIdentifiers.bike256, new BIKEConverter());
        converters.put(BCObjectIdentifiers.hqc128, new HQCConverter());
        converters.put(BCObjectIdentifiers.hqc192, new HQCConverter());
        converters.put(BCObjectIdentifiers.hqc256, new HQCConverter());
    }

    /**
     * Create a public key from a SubjectPublicKeyInfo encoding
     *
     * @param keyInfoData the SubjectPublicKeyInfo encoding
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(byte[] keyInfoData)
        throws IOException
    {
        if (keyInfoData == null)
        {
            throw new IllegalArgumentException("keyInfoData array null");
        }
        if (keyInfoData.length == 0)
        {
            throw new IllegalArgumentException("keyInfoData array empty");
        }
        return createKey(SubjectPublicKeyInfo.getInstance(ASN1Primitive.fromByteArray(keyInfoData)));
    }

    /**
     * Create a public key from a SubjectPublicKeyInfo encoding read from a stream
     *
     * @param inStr the stream to read the SubjectPublicKeyInfo encoding from
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(InputStream inStr)
        throws IOException
    {
        return createKey(SubjectPublicKeyInfo.getInstance(new ASN1InputStream(inStr).readObject()));
    }

    /**
     * Create a public key from the passed in SubjectPublicKeyInfo
     *
     * @param keyInfo the SubjectPublicKeyInfo containing the key data
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(SubjectPublicKeyInfo keyInfo)
        throws IOException
    {
        if (keyInfo == null)
        {
            throw new IllegalArgumentException("keyInfo argument null");
        }
        return createKey(keyInfo, null);
    }

    /**
     * Create a public key from the passed in SubjectPublicKeyInfo
     *
     * @param keyInfo       the SubjectPublicKeyInfo containing the key data
     * @param defaultParams default parameters that might be needed.
     * @return the appropriate key parameter
     * @throws IOException on an error decoding the key
     */
    public static AsymmetricKeyParameter createKey(SubjectPublicKeyInfo keyInfo, Object defaultParams)
        throws IOException
    {
        if (keyInfo == null)
        {
            throw new IllegalArgumentException("keyInfo argument null");
        }

        AlgorithmIdentifier algId = keyInfo.getAlgorithm();
        SubjectPublicKeyInfoConverter converter = (SubjectPublicKeyInfoConverter)converters.get(algId.getAlgorithm());

        if (converter != null)
        {
            return converter.getPublicKeyParameters(keyInfo, defaultParams);
        }
        else
        {
            throw new IOException("algorithm identifier in public key not recognised: " + algId.getAlgorithm());
        }
    }

    private static abstract class SubjectPublicKeyInfoConverter
    {
        abstract AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
            throws IOException;
    }

    private static class NHConverter
        extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
            throws IOException
        {
            return new NHPublicKeyParameters(keyInfo.getPublicKeyData().getBytes());
        }
    }

    private static class CMCEConverter
        extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
            throws IOException
        {
            try
            {
                byte[] keyEnc = CMCEPublicKey.getInstance(keyInfo.parsePublicKey()).getT();

                CMCEParameters spParams = Utils.mcElieceParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

                return new CMCEPublicKeyParameters(spParams, keyEnc);
            }
            catch (Exception e)
            {        
                byte[] keyEnc = keyInfo.getPublicKeyData().getOctets();

                CMCEParameters spParams = Utils.mcElieceParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

                return new CMCEPublicKeyParameters(spParams, keyEnc);
            }
        }
    }

    private static class SABERConverter
            extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
                throws IOException
        {
            byte[] keyEnc = ASN1OctetString.getInstance(
                    ASN1Sequence.getInstance(keyInfo.parsePublicKey()).getObjectAt(0)).getOctets();

            SABERParameters saberParams = Utils.saberParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

            return new SABERPublicKeyParameters(saberParams, keyEnc);
        }
    }

    private static class FrodoConverter
            extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
                throws IOException
        {
            byte[] keyEnc = ASN1OctetString.getInstance(keyInfo.parsePublicKey()).getOctets();

            FrodoParameters fParams = Utils.frodoParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

            return new FrodoPublicKeyParameters(fParams, keyEnc);
        }
    }

    private static class PicnicConverter
            extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
                throws IOException
        {
            byte[] keyEnc = ASN1OctetString.getInstance(keyInfo.parsePublicKey()).getOctets();

            PicnicParameters picnicParams = Utils.picnicParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

            return new PicnicPublicKeyParameters(picnicParams, keyEnc);
        }
    }

    private static class FalconConverter
        extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
            throws IOException
        {
            byte[] keyEnc = keyInfo.getPublicKeyData().getOctets();
//            FalconPublicKey falconPublicKey = FalconPublicKey.getInstance(keyInfo.parsePublicKey());

            FalconParameters falconParams = Utils.falconParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

            return new FalconPublicKeyParameters(falconParams, Arrays.copyOfRange(keyEnc, 1, keyEnc.length));

        }
    }

    private static class KyberConverter
        extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
            throws IOException
        {
            KyberParameters kyberParameters = Utils.kyberParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

            try
            {
                ASN1Primitive obj = keyInfo.parsePublicKey();
                KyberPublicKey kyberKey = KyberPublicKey.getInstance(obj);

                return new KyberPublicKeyParameters(kyberParameters, kyberKey.getT(), kyberKey.getRho());
            }
            catch (Exception e)
            {
                // we're a raw encoding
                return new KyberPublicKeyParameters(kyberParameters, keyInfo.getPublicKeyData().getOctets());
            }
        }
    }

    static class DilithiumConverter
        extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
            throws IOException
        {
            DilithiumParameters dilithiumParams = Utils.dilithiumParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

            return getPublicKeyParams(dilithiumParams, keyInfo.getPublicKeyData());
        }

        static DilithiumPublicKeyParameters getPublicKeyParams(DilithiumParameters dilithiumParams, ASN1BitString publicKeyData)
        {
            try
            {
                ASN1Primitive obj = ASN1Primitive.fromByteArray(publicKeyData.getOctets());
                if (obj instanceof ASN1Sequence)
                {
                    ASN1Sequence keySeq = ASN1Sequence.getInstance(obj);

                    return new DilithiumPublicKeyParameters(dilithiumParams,
                        ASN1OctetString.getInstance(keySeq.getObjectAt(0)).getOctets(),
                        ASN1OctetString.getInstance(keySeq.getObjectAt(1)).getOctets());
                }
                else
                {
                    byte[] encKey = ASN1OctetString.getInstance(obj).getOctets();

                    return new DilithiumPublicKeyParameters(dilithiumParams, encKey);
                }
            }
            catch (Exception e)
            {
                // we're a raw encoding
                return new DilithiumPublicKeyParameters(dilithiumParams, publicKeyData.getOctets());
            }
        }
    }

    private static class BIKEConverter
            extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
                throws IOException
        {
            try
            {
                byte[] keyEnc = ASN1OctetString.getInstance(keyInfo.parsePublicKey()).getOctets();

                BIKEParameters bikeParams = Utils.bikeParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

                return new BIKEPublicKeyParameters(bikeParams, keyEnc);
            }
            catch (Exception e)
            {
                byte[] keyEnc = keyInfo.getPublicKeyData().getOctets();

                BIKEParameters bikeParams = Utils.bikeParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

                return new BIKEPublicKeyParameters(bikeParams, keyEnc);
            }
        }
    }

    private static class HQCConverter
            extends SubjectPublicKeyInfoConverter
    {
        AsymmetricKeyParameter getPublicKeyParameters(SubjectPublicKeyInfo keyInfo, Object defaultParams)
                throws IOException
        {
            try
            {
                byte[] keyEnc = ASN1OctetString.getInstance(keyInfo.parsePublicKey()).getOctets();

                HQCParameters hqcParams = Utils.hqcParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

                return new HQCPublicKeyParameters(hqcParams, keyEnc);
            }
            catch (Exception e)
            {
                // raw encoding
                byte[] keyEnc = keyInfo.getPublicKeyData().getOctets();

                HQCParameters hqcParams = Utils.hqcParamsLookup(keyInfo.getAlgorithm().getAlgorithm());

                return new HQCPublicKeyParameters(hqcParams, keyEnc);
            }
        }
    }
}
