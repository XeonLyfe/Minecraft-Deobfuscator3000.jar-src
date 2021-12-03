package org.ugp.mc.deobfuscator;

import com.strobel.assembler.ir.ConstantPool;
import com.strobel.assembler.metadata.Buffer;
import com.strobel.assembler.metadata.ClasspathTypeLoader;
import com.strobel.assembler.metadata.ITypeLoader;
import com.strobel.core.StringComparison;
import com.strobel.core.StringUtilities;
import com.strobel.io.PathHelper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class ClassTypeLoader
implements ITypeLoader {
    private final ITypeLoader _defaultTypeLoader = new ClasspathTypeLoader();
    private final Map<String, LinkedHashSet<File>> _packageLocations = new LinkedHashMap<String, LinkedHashSet<File>>();
    private final Map<String, File> _knownFiles = new LinkedHashMap<String, File>();

    @Override
    public boolean tryLoadType(String typeNameOrPath, Buffer buffer) {
        String internalName;
        boolean hasExtension = StringUtilities.endsWithIgnoreCase(typeNameOrPath, ".class");
        if (hasExtension && this.tryLoadFile(null, typeNameOrPath, buffer, true)) {
            return true;
        }
        String string = internalName = hasExtension ? typeNameOrPath.substring(0, typeNameOrPath.length() - 6) : typeNameOrPath.replace('.', '/');
        if (this.tryLoadTypeFromName(internalName, buffer)) {
            return true;
        }
        if (hasExtension) {
            return false;
        }
        return false;
    }

    private boolean tryLoadTypeFromName(String internalName, Buffer buffer) {
        if (this.tryLoadFromKnownLocation(internalName, buffer)) {
            return true;
        }
        if (this._defaultTypeLoader.tryLoadType(internalName, buffer)) {
            return true;
        }
        String filePath = String.valueOf(internalName.replace('/', File.separatorChar)) + ".class";
        int lastSeparatorIndex = filePath.lastIndexOf(File.separatorChar);
        return lastSeparatorIndex >= 0 && this.tryLoadFile(internalName, filePath.substring(lastSeparatorIndex + 1), buffer, true);
    }

    private boolean tryLoadFromKnownLocation(String internalName, Buffer buffer) {
        String tail;
        String head;
        File knownFile = this._knownFiles.get(internalName);
        if (knownFile != null && this.tryLoadFile(knownFile, buffer)) {
            return true;
        }
        int packageEnd = internalName.lastIndexOf(47);
        if (packageEnd < 0 || packageEnd >= internalName.length()) {
            head = "";
            tail = internalName;
        } else {
            head = internalName.substring(0, packageEnd);
            tail = internalName.substring(packageEnd + 1);
        }
        while (true) {
            int split;
            LinkedHashSet<File> directories;
            if ((directories = this._packageLocations.get(head)) != null) {
                for (File directory : directories) {
                    if (!this.tryLoadFile(internalName, new File(directory, String.valueOf(tail) + ".class").getAbsolutePath(), buffer, true)) continue;
                    return true;
                }
            }
            if ((split = head.lastIndexOf(47)) <= 0) break;
            tail = String.valueOf(head.substring(split + 1)) + '/' + tail;
            head = head.substring(0, split);
        }
        return false;
    }

    private boolean tryLoadFile(File file, Buffer buffer) {
        block14: {
            if (!file.exists() || file.isDirectory()) {
                return false;
            }
            Throwable throwable = null;
            Object var4_6 = null;
            FileInputStream in = new FileInputStream(file);
            try {
                int remainingBytes = in.available();
                buffer.position(0);
                buffer.reset(remainingBytes);
                while (remainingBytes > 0) {
                    int bytesRead = in.read(buffer.array(), buffer.position(), remainingBytes);
                    if (bytesRead < 0) break;
                    remainingBytes -= bytesRead;
                    buffer.advance(bytesRead);
                }
                buffer.position(0);
                if (in == null) break block14;
            }
            catch (Throwable throwable2) {
                try {
                    try {
                        if (in != null) {
                            in.close();
                        }
                        throw throwable2;
                    }
                    catch (Throwable throwable3) {
                        if (throwable == null) {
                            throwable = throwable3;
                        } else if (throwable != throwable3) {
                            throwable.addSuppressed(throwable3);
                        }
                        throw throwable;
                    }
                }
                catch (IOException e) {
                    return false;
                }
            }
            in.close();
        }
        return true;
    }

    private boolean tryLoadFile(String internalName, String typeNameOrPath, Buffer buffer, boolean trustName) {
        boolean result;
        String name;
        File file = new File(typeNameOrPath);
        if (!this.tryLoadFile(file, buffer)) {
            return false;
        }
        String actualName = ClassTypeLoader.getInternalNameFromClassFile(buffer);
        String string = trustName ? (internalName != null ? internalName : actualName) : (name = actualName);
        if (name == null) {
            return false;
        }
        boolean nameMatches = StringUtilities.equals(actualName, internalName);
        boolean pathMatchesName = typeNameOrPath.endsWith(String.valueOf(name.replace('/', File.separatorChar)) + ".class");
        boolean bl = result = internalName == null || pathMatchesName || nameMatches;
        if (result) {
            int packageEnd = name.lastIndexOf(47);
            String packageName = packageEnd < 0 || packageEnd >= name.length() ? "" : name.substring(0, packageEnd);
            this.registerKnownPath(packageName, file.getParentFile(), pathMatchesName);
            this._knownFiles.put(actualName, file);
        } else {
            buffer.reset(0);
        }
        return result;
    }

    private void registerKnownPath(String packageName, File directory, boolean recursive) {
        if (directory == null || !directory.exists()) {
            return;
        }
        LinkedHashSet<File> directories = this._packageLocations.get(packageName);
        if (directories == null) {
            directories = new LinkedHashSet();
            this._packageLocations.put(packageName, directories);
        }
        if (!directories.add(directory) || !recursive) {
            return;
        }
        try {
            int delimiterIndex;
            String directoryPath = StringUtilities.removeRight(directory.getCanonicalPath(), new char[]{PathHelper.DirectorySeparator, PathHelper.AlternateDirectorySeparator}).replace('\\', '/');
            String currentPackage = packageName;
            File currentDirectory = new File(directoryPath);
            while ((delimiterIndex = currentPackage.lastIndexOf(47)) >= 0 && currentDirectory.exists() && delimiterIndex < currentPackage.length() - 1) {
                String segmentName = currentPackage.substring(delimiterIndex + 1);
                if (StringUtilities.equals(currentDirectory.getName(), segmentName, StringComparison.OrdinalIgnoreCase)) {
                    currentPackage = currentPackage.substring(0, delimiterIndex);
                    currentDirectory = currentDirectory.getParentFile();
                    directories = this._packageLocations.get(currentPackage);
                    if (directories == null) {
                        directories = new LinkedHashSet();
                        this._packageLocations.put(currentPackage, directories);
                    }
                    if (directories.add(currentDirectory)) {
                        continue;
                    }
                }
                break;
            }
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    private static String getInternalNameFromClassFile(Buffer b) {
        long magic = (long)b.readInt() & 0xFFFFFFFFL;
        if (magic != 3405691582L) {
            return null;
        }
        b.readUnsignedShort();
        b.readUnsignedShort();
        ConstantPool constantPool = ConstantPool.read(b);
        b.readUnsignedShort();
        ConstantPool.TypeInfoEntry thisClass = (ConstantPool.TypeInfoEntry)constantPool.getEntry(b.readUnsignedShort());
        b.position(0);
        return thisClass.getName();
    }
}
