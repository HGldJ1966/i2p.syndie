package syndie.db;

import java.io.*;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import net.i2p.I2PAppContext;
import net.i2p.crypto.KeyGenerator;
import net.i2p.crypto.SHA256Generator;
import net.i2p.data.Base64;
import net.i2p.data.DataHelper;
import net.i2p.data.SessionKey;
import net.i2p.data.PrivateKey;
import net.i2p.data.PublicKey;
import net.i2p.data.SigningPrivateKey;
import net.i2p.data.SigningPublicKey;
import net.i2p.data.Signature;
import net.i2p.data.Hash;
import net.i2p.util.SecureFile;
import net.i2p.util.SecureFileOutputStream;

import syndie.Constants;
import syndie.data.ChannelInfo;
import syndie.data.EnclosureBody;
import syndie.data.NymKey;
import syndie.data.ReferenceNode;

/**
 *changen
 * [--channelId $internalId]        // if set, try to update the given channel rather than create a new one (only if authorized)
 *  --name $name
 * [--description $desc]
 * [--avatar $filename|$base64AvatarPNG]             // location of 32x32 PNG formatted avatar
 * [--edition $num]                 // edition to publish, or an automatically chosen value if not specified
 * [--publicPosting $boolean]       // can anyone create new threads?
 * [--publicReplies $boolean]       // can anyone reply to posts?
 * [--pubTag $tag]*
 * [--privTag $tag]*
 * [--postKey $base64PubKey]*       // who is allowed to post to the channel
 * [--manageKey $base64PubKey]*     // who is allowed to manage the channel
 * [--refs ($channelRefGroupFile|$channelRefGroupContent)]    // ([\t]*$name\t$uri\t$refType\t$description\n)* lines
 * [--pubArchive $archive]*
 * [--privArchive $archive]*
 * [--encryptContent $boolean]      // don't publicize the key encrypting the metadata, and include a session key in the encrypted metadata to read posts with
 * [--bodyPassphrase $passphrase --bodyPassphrasePrompt $prompt]
 *                                  // derive the body key from the passphrase, and include a publicly
 *                                  // visible hint to prompt it
 * [--deliverReadKeys $base64SessionKey]*
 *                                  // we want to make sure to include the specified channel read key in the
 *                                  // wrapped metadata
 * [--explicitBodyKey $base64SessionKey]
 *                                  // explicitly specify the read key to use when encrypting the body
 * [--createReplyKey $boolean]      // if true, create a new reply key
 *  --metaOut $metadataFile         // signed metadata file, ready to import
 *  --keyManageOut $keyFile         // signing private key to manage
 *  --keyReplyOut $keyFile          // decrypt private key to read replies
 * [--keyEncryptPostOut $keyFile]   // key used to encrypt posts (may be hidden if --encryptContent, otherwise anyone can get it too)
 * [--keyEncryptMetaOut $keyFile]   // key used to encrypt metadata (if --encryptContent)
 * [--cancelledURI $syndieURI]*     // uris to cancel
 */
public class ChanGen extends CommandImpl {
    private I2PAppContext _ctx;
    public ChanGen(I2PAppContext ctx) { _ctx = ctx; }
    public ChanGen() { this(I2PAppContext.getGlobalContext()); }
    
    static final boolean CLONEABLE_DIGEST;
    static {
        boolean ok = false;
        try {
            MessageDigest md = SHA256Generator.getDigestInstance();
            md.update((byte) 42);
            md.clone();
            ok = true;
        } catch (CloneNotSupportedException e) {}
        CLONEABLE_DIGEST = ok;
    }

    public static String getHelp(String cmd) {
        return "--name $name --keyManageOut $keyFile --keyReplyOut $keyFile (see source for more options)";
    }

    public DBClient runCommand(Opts args, UI ui, DBClient client) {
        List missing = args.requireOpts(new String[] { "name", /*"metaOut",*/ "keyManageOut", "keyReplyOut" });
        if (missing.size() > 0) {
            ui.errorMessage("Invalid options, missing " + missing);
            ui.commandComplete(-1, null);
            return client;
        }
        
        if (args.getOptBoolean("encryptContent", false) && (args.getOptValue("keyEncryptPostOut") == null) ) {
            ui.errorMessage("When posts should be encrypted, you probably want to generate a key they should use, 'eh?");
            ui.errorMessage("(so either use --keyEncryptPostOut $outFile or use --encryptContent false)");
            ui.commandComplete(-1, null);
            return client;
        }
        
        Object repKeys[] = _ctx.keyGenerator().generatePKIKeypair();
        PublicKey replyPublic = (PublicKey)repKeys[0];
        PrivateKey replyPrivate = (PrivateKey)repKeys[1];
        Object identKeys[] = _ctx.keyGenerator().generateSigningKeypair();
        SigningPublicKey identPublic = (SigningPublicKey)identKeys[0];
        SigningPrivateKey identPrivate = (SigningPrivateKey)identKeys[1];
        SessionKey bodyKey = _ctx.keyGenerator().generateSessionKey();
        SessionKey readKey = _ctx.keyGenerator().generateSessionKey(); // not always used
        
        String out = args.getOptValue("metaOut");
        if (out == null) {
            File chanDir = new SecureFile(client.getOutboundDir(), identPublic.calculateHash().toBase64());
            chanDir.mkdirs();
            out = new File(chanDir, "meta" + Constants.FILENAME_SUFFIX).getPath();
        }
        
        long existingChannelId = args.getOptLong("channelId", -1);
        if (existingChannelId >= 0) {
            ChannelInfo existing = client.getChannel(existingChannelId);
            if (existing == null) {
                ui.errorMessage("Cannot update the channel " + existingChannelId + ", as it is not known?");
                ui.commandComplete(-1, null);
                return client;
            }
            PublicKey enc = existing.getEncryptKey();
            PrivateKey encPriv = null;
            List keys = client.getNymKeys(client.getLoggedInNymId(), client.getPass(), existing.getChannelHash(), Constants.KEY_FUNCTION_REPLY);
            if ( (keys != null) && (keys.size() >= 0) ) {
                for (int i = 0; i < keys.size(); i++) {
                    NymKey k = (NymKey)keys.get(i);
                    PrivateKey priv = new PrivateKey(k.getData());
                    PublicKey curPub = KeyGenerator.getPublicKey(priv);
                    if (curPub.equals(enc)) {
                        encPriv = priv;
                        break;
                    }
                }
            }
            SigningPublicKey ident = existing.getIdentKey();
            SigningPrivateKey identPriv = null;
            keys = client.getNymKeys(client.getLoggedInNymId(), client.getPass(), existing.getChannelHash(), Constants.KEY_FUNCTION_MANAGE);
            if ( (keys != null) && (keys.size() >= 0) ) {
                for (int i = 0; i < keys.size(); i++) {
                    NymKey k = (NymKey)keys.get(i);
                    SigningPrivateKey priv = new SigningPrivateKey(k.getData());
                    SigningPublicKey curPub = KeyGenerator.getSigningPublicKey(priv);
                    if (curPub.equals(ident)) {
                        identPriv = priv;
                        break;
                    }
                }
            }
            
            if (identPriv == null) {
                ui.errorMessage("Not authorized to update the channel " + ident.calculateHash().toBase64());
                ui.commandComplete(-1, null);
                return client;
            }
            
            identPublic = ident;
            identPrivate = identPriv;
            if (!args.getOptBoolean("createReplyKey", false)) {
                replyPublic = enc;
                replyPrivate = encPriv; // may be null, in case we are allowed to manage but not receive replies
            } else {
                // rotating keys
            }
        
            keys = client.getNymKeys(client.getLoggedInNymId(), client.getPass(), existing.getChannelHash(), Constants.KEY_FUNCTION_READ);
            if ( (keys != null) && (keys.size() > 0) ) {
                int idx = client.ctx().random().nextInt(keys.size());
                NymKey k = (NymKey)keys.get(idx);
                bodyKey = new SessionKey(k.getData());
                readKey = new SessionKey(k.getData());
            } else {
                // use the channel's default read keys
                /*
                 * todo: depending on whether we want to make this forum publicly readable or
                 *       just authorized readers (or pbe-only), pick different keys (or if the
                 *       privacy level changed, use new keys anyway, so those under the old
                 *       privacy level (or with an old passphrase) can't access new posts).
                 *       or, let the higher level decide this stuff and tell us what to do with
                 *       --deliverReadKeys and --explicitBodyKey
                 */
                Set readKeys = existing.getReadKeys();
                ui.debugMessage("using the channel's read keys: " + readKeys + " (channel: " + existing.getChannelHash() + ")");
                if (readKeys.size() > 0) {
                    int idx = client.ctx().random().nextInt(readKeys.size());
                    SessionKey cur = null;
                    Iterator iter = readKeys.iterator();
                    for (int i = 0; i < idx; i++)
                        iter.next(); // ignore
                    cur = (SessionKey)iter.next();
                    bodyKey = cur;
                    readKey = cur;
                } else {
                    // we use a new one generated above
                }
            }
        
        }
    
        if (true) {
            SigningPublicKey testPub = KeyGenerator.getSigningPublicKey(identPrivate);
            if (identPublic.equals(testPub)) {
                // ok, gravity works
            } else {
                ui.errorMessage("Signing private key b0rked: " + identPrivate.toBase64());
                ui.errorMessage("It generates a public key: " + testPub.toBase64());
                ui.errorMessage("that does not match the orig pub key: " + identPublic.toBase64());
                ui.commandComplete(-1, null);
                return client;
            }
        }
        
        List readKeys = new ArrayList();
        List deliver = args.getOptValues("deliverReadKeys");
        if (deliver != null) {
            for (int i = 0; i < deliver.size(); i++) {
                String str = (String)deliver.get(i);
                byte k[] = Base64.decode(str);
                if ( (k != null) && (k.length == SessionKey.KEYSIZE_BYTES) ) {
                    SessionKey toAdd = new SessionKey(k);
                    if (!readKeys.contains(toAdd))
                        readKeys.add(toAdd);
                }
            }
            ui.debugMessage("Explicitly including a set of read keys: " + readKeys);
            readKey = null;
        } else {
            // the read key wasn't explicitly selected, so use the one determined by
            // the above logic
            readKeys.add(readKey);
            ui.debugMessage("Using implicit read keys: " + readKeys);
        }
        
        String explicitBody = args.getOptValue("explicitBodyKey");
        if (explicitBody != null) {
            byte k[] = Base64.decode(explicitBody);
            if ( (k != null) && (k.length == SessionKey.KEYSIZE_BYTES) ) {
                SessionKey newBody = new SessionKey(k);
                bodyKey = newBody;
                ui.debugMessage("Explicitly selecting the body key: " + bodyKey);
            }
        }
        
        Map pubHeaders = generatePublicHeaders(ui, args, replyPublic, identPublic, bodyKey, readKey);
        Map privHeaders = generatePrivateHeaders(ui, args, replyPublic, identPublic, bodyKey, readKeys);
        
        String refStr = null;
        String filename = args.getOptValue("refs");
        if (filename != null) {
            FileInputStream fin = null;
            File f = new File(filename);
            if (f.exists()) {
                try {
                    fin = new FileInputStream(f);
                    List refNodes = ReferenceNode.buildTree(fin);
                    refStr = ReferenceNode.walk(refNodes);
                } catch (IOException ioe) {
                    ui.errorMessage("Error pulling in the refs", ioe);
                    ui.commandComplete(-1, null);
                    return client;
                } finally {
                    if (fin != null) try { fin.close(); } catch (IOException ioe) {}
                }
            } else {
                refStr = filename;
            }
        }
        
        String avatarStr = args.getOptValue("avatar");
        byte avatar[] = null;
        if (avatarStr != null) {
            File fname = new File(avatarStr);
            if (fname.exists())
                avatar = read(ui, avatarStr, Constants.MAX_AVATAR_SIZE);
            else
                avatar = Base64.decode(avatarStr);
        }
        
        boolean ok = writeMeta(ui, out, refStr, identPublic, identPrivate, bodyKey, pubHeaders, privHeaders, avatar);
        if (ok)
            ok = writeKey(ui, args.getOptValue("keyManageOut"), identPrivate, identPublic.calculateHash());
        if (ok && (replyPrivate != null))
            ok = writeKey(ui, args.getOptValue("keyReplyOut"), replyPrivate, identPublic.calculateHash());
        if (ok && (args.getOptBoolean("encryptContent", false))) {
            ok = writeKey(ui, args.getOptValue("keyEncryptMetaOut"), bodyKey, identPublic.calculateHash());
            if (readKey != null)
                ok = ok && writeKey(ui, args.getOptValue("keyEncryptPostOut"), readKey, identPublic.calculateHash());
        }
        if (ok)
            ui.commandComplete(0, null);
        else
            ui.commandComplete(-1, null);
        
        return client;
    }
    
    private Map generatePublicHeaders(UI ui, Opts args, PublicKey replyPublic, SigningPublicKey identPublic, SessionKey bodyKey, SessionKey readKey) {
        Map rv = new HashMap();
        
        rv.put(Constants.MSG_HEADER_TYPE, Constants.MSG_TYPE_META);
        
        // tags
        List tags = args.getOptValues("pubTag");
        if ( (tags != null) && (tags.size() > 0) ) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < tags.size(); i++)
                buf.append(strip((String)tags.get(i))).append('\t');
            rv.put(Constants.MSG_META_HEADER_TAGS, buf.toString());
        }
        
        // cancelled URIs
        List cancelledURIs = args.getOptValues("cancelledURI");
        if ( (cancelledURIs != null) && (cancelledURIs.size() > 0) ) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < cancelledURIs.size(); i++)
                buf.append(strip((String)cancelledURIs.get(i))).append('\t');
            rv.put(Constants.MSG_META_HEADER_CANCEL, buf.toString());
        }
        
        // ident
        rv.put(Constants.MSG_META_HEADER_IDENTITY, identPublic.toBase64());
        // reply
        rv.put(Constants.MSG_META_HEADER_ENCRYPTKEY, replyPublic.toBase64());
        // edition, defaulting to 0 (should this instead default to trunc(now(), yyyy/mm)?)
        rv.put(Constants.MSG_META_HEADER_EDITION, Long.toString(args.getOptLong("edition", 0)));
        if ( (args.getOptValue("bodyPassphrase") != null) && (args.getOptValue("bodyPassphrasePrompt") != null) ) {
            String passphrase = strip(args.getOptValue("bodyPassphrase"));
            byte salt[] = new byte[32];
            _ctx.random().nextBytes(salt);
            SessionKey pbeKey = _ctx.keyGenerator().generateSessionKey(salt, DataHelper.getUTF8(passphrase));
            bodyKey.setData(pbeKey.getData());
            String prompt = strip(args.getOptValue("bodyPassphrasePrompt"));
            rv.put(Constants.MSG_HEADER_PBE_PROMPT, prompt);
            rv.put(Constants.MSG_HEADER_PBE_PROMPT_SALT, Base64.encode(salt));
        } else if (!args.getOptBoolean("encryptContent", false)) {
            // if we are NOT trying to privately encrypt the content, then publicize the bodyKey in the public
            // headers (so anyone can open the zip content and read the private headers/refs/avatar/etc)
            //rv.put(Constants.MSG_META_HEADER_POST_KEYS, readKey.toBase64()); // keep in the private headers
            rv.put(Constants.MSG_HEADER_BODYKEY, bodyKey.toBase64());
        }
        // can any authenticated (yet not necessarily authorized) post go through?
        if (args.getOptBoolean("publicPosting", false))
            rv.put(Constants.MSG_META_HEADER_PUBLICPOSTING, "true");
        // can any authenticated (yet not necessarily authorized) reply to an existing post go through?
        if (args.getOptBoolean("publicReplies", false))
            rv.put(Constants.MSG_META_HEADER_PUBLICREPLY, "true");
        // what keys can authorize posts (in addition to the channel ident key, of course)
        List auth = args.getOptValues("postKey");
        if ( (auth != null) && (auth.size() > 0) ) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < auth.size(); i++)
                buf.append(strip((String)auth.get(i))).append('\t');
            rv.put(Constants.MSG_META_HEADER_POST_KEYS, buf.toString());
        }
        // what keys can create new metadata messages (in addition to the channel ident key, of course)
        List manage = args.getOptValues("manageKey");
        if ( (manage != null) && (manage.size() > 0) ) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < manage.size(); i++)
                buf.append(strip((String)manage.get(i))).append('\t');
            rv.put(Constants.MSG_META_HEADER_MANAGER_KEYS, buf.toString());
        }
        // publicly visible archives of this channel
        List archives = args.getOptValues("pubArchive");
        if ( (archives != null) && (archives.size() > 0) ) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < archives.size(); i++)
                buf.append(strip((String)archives.get(i))).append('\t');
            rv.put(Constants.MSG_META_HEADER_ARCHIVES, buf.toString());
        }
        
        ui.debugMessage("public headers: " + rv);
        return rv;
    }
    private Map generatePrivateHeaders(UI ui, Opts args, PublicKey replyPublic, SigningPublicKey identPublic, SessionKey bodyKey, List readKeys) {
        Map rv = new HashMap();

        StringBuilder rkbuf = new StringBuilder();
        for (int i = 0; i < readKeys.size(); i++) {
            SessionKey cur = (SessionKey)readKeys.get(i);
            rkbuf.append(cur.toBase64());
            if (i + 1 < readKeys.size())
                rkbuf.append('\t');
        }
        rv.put(Constants.MSG_META_HEADER_READKEYS, rkbuf.toString());
        
        // tags
        List tags = args.getOptValues("privTag");
        if ( (tags != null) && (tags.size() > 0) ) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < tags.size(); i++)
                buf.append(strip((String)tags.get(i))).append('\t');
            rv.put(Constants.MSG_META_HEADER_TAGS, buf.toString());
        }
        
        // name
        String name = args.getOptValue("name");
        if (name != null)
            rv.put(Constants.MSG_META_HEADER_NAME, strip(name));
        // description
        String desc = args.getOptValue("description");
        if (desc != null)
            rv.put(Constants.MSG_META_HEADER_DESCRIPTION, strip(desc));
        
        // private archives of this channel
        List archives = args.getOptValues("privArchive");
        if ( (archives != null) && (archives.size() > 0) ) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < archives.size(); i++)
                buf.append(strip((String)archives.get(i))).append('\t');
            rv.put(Constants.MSG_META_HEADER_ARCHIVES, buf.toString());
        }
        
        ui.debugMessage("private headers: " + rv);
        return rv;
    }

    @SuppressWarnings("deprecation")
    private boolean writeMeta(UI ui, String metaOut, String refStr, SigningPublicKey identPublic, SigningPrivateKey identPrivate, SessionKey bodyKey, Map pubHeaders, Map privHeaders, byte avatar[]) {
        FileOutputStream fos = null;
        try {
            byte encBody[] = encryptBody(_ctx, writeRawBody(refStr, privHeaders, avatar), bodyKey);
            fos = new SecureFileOutputStream(metaOut);
            // we need two hashes because we don't know if
            // MessageDigest supports clone()
            // authorization hash
            MessageDigest hash = SHA256Generator.getDigestInstance();
            // authentication hash
            MessageDigest hash2;
            if (CLONEABLE_DIGEST)
                hash2 = hash;
            else
                hash2 = SHA256Generator.getDigestInstance();
            byte[] data = DataHelper.getUTF8(Constants.TYPE_CURRENT + '\n');
            DataHelper.write(fos, data, hash);
            if (!CLONEABLE_DIGEST)
                hash2.update(data);
            TreeSet ordered = new TreeSet(pubHeaders.keySet());
            for (Iterator iter = ordered.iterator(); iter.hasNext(); ) {
                String key = (String)iter.next();
                String val = (String)pubHeaders.get(key);
                data = DataHelper.getUTF8(key + '=' + val + '\n');
                DataHelper.write(fos, data, hash);
                if (!CLONEABLE_DIGEST)
                    hash2.update(data);
            }
            data = DataHelper.getUTF8("\nSize=" + encBody.length + '\n');
            DataHelper.write(fos, data, hash);
            if (!CLONEABLE_DIGEST)
                hash2.update(data);
            DataHelper.write(fos, encBody, hash);
            if (!CLONEABLE_DIGEST)
                hash2.update(encBody);
            
            byte[] authorizationHash;
            if (CLONEABLE_DIGEST) {
                try {
                    authorizationHash = ((MessageDigest) hash.clone()).digest();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException("shouldn't happen", e);
                }
            } else {
                authorizationHash = hash.digest();
            }
            Signature authorizationSig = _ctx.dsa().sign(new Hash(authorizationHash), identPrivate);
            ui.debugMessage("Authorization hash: " + Base64.encode(authorizationHash) + " sig: " + authorizationSig.toBase64());
            DataHelper.write(fos, DataHelper.getUTF8("AuthorizationSig=" + authorizationSig.toBase64() + '\n'), hash2);
            
            byte authenticationHash[] = hash2.digest();
            Signature authenticationSig = _ctx.dsa().sign(new Hash(authenticationHash), identPrivate);
            ui.debugMessage("Authentication hash: " + Base64.encode(authenticationHash) + " sig: " + authenticationSig.toBase64());
            fos.write(DataHelper.getUTF8("AuthenticationSig=" + authenticationSig.toBase64() + '\n'));
            
            fos.close();
            fos = null;
            return true;
        } catch (IOException ioe) {
            ui.errorMessage("Error writing the meta", ioe);
            ui.commandComplete(-1, null);
            return false;
        } finally {
            if (fos != null) try { fos.close(); } catch (IOException ioe) {}
        }
    }
    
    private byte[] writeRawBody(String refStr, Map privHeaders, byte avatar[]) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4*1024);
        ZipOutputStream zos = new ZipOutputStream(baos);
        if ( (privHeaders != null) && (privHeaders.size() > 0) ) {
            ZipEntry entry = new ZipEntry(EnclosureBody.ENTRY_HEADERS);
            entry.setTime(0);
            zos.putNextEntry(entry);
            write(privHeaders, zos);
            zos.closeEntry();
        }
        if ( (avatar != null) && (avatar.length > 0) ) {
            ZipEntry entry = new ZipEntry(EnclosureBody.ENTRY_AVATAR);
            entry.setTime(0);
            entry.setSize(avatar.length);
            zos.putNextEntry(entry);
            zos.write(avatar);
            zos.closeEntry();
        }
        if (refStr != null) {
            ZipEntry entry = new ZipEntry(EnclosureBody.ENTRY_REFERENCES);
            entry.setTime(0);
            byte ref[] = DataHelper.getUTF8(refStr);
            entry.setSize(ref.length);
            zos.putNextEntry(entry);
            zos.write(ref);
            zos.closeEntry();
        }
        zos.close();
        
        byte raw[] = baos.toByteArray();
        return raw;
    }
    
/****
    public static void main(String args[]) {
        try {
        CLI.main(new String[] { "changen", 
                                "--name", "my name", 
                                "--description", "this is my channel",
                                "--privTag", "tag1",
                                "--privTag", "tag2",
                                "--privTag", "tag3",
                                "--metaOut", "/tmp/metaOut",
                                "--keyManageOut", "/tmp/manageOut",
                                "--keyReplyOut", "/tmp/replyOut"});
        } catch (Exception e) { e.printStackTrace(); }
    }
****/
}
