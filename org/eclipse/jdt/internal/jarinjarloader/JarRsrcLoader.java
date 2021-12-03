package org.eclipse.jdt.internal.jarinjarloader;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import org.eclipse.jdt.internal.jarinjarloader.RsrcURLStreamHandlerFactory;

public class JarRsrcLoader {
    public static void main(String[] args) throws ClassNotFoundException, IllegalArgumentException, IllegalAccessException, InvocationTargetException, SecurityException, NoSuchMethodException, IOException {
        ManifestInfo mi = JarRsrcLoader.getManifestInfo();
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        URL.setURLStreamHandlerFactory(new RsrcURLStreamHandlerFactory(cl));
        URL[] rsrcUrls = new URL[mi.rsrcClassPath.length];
        int i = 0;
        while (i < mi.rsrcClassPath.length) {
            String rsrcPath = mi.rsrcClassPath[i];
            rsrcUrls[i] = rsrcPath.endsWith("/") ? new URL("rsrc:" + rsrcPath) : new URL("jar:rsrc:" + rsrcPath + "!/");
            ++i;
        }
        URLClassLoader jceClassLoader = new URLClassLoader(rsrcUrls, JarRsrcLoader.getParentClassLoader());
        Thread.currentThread().setContextClassLoader(jceClassLoader);
        Class<?> c = Class.forName(mi.rsrcMainClass, true, jceClassLoader);
        Method main = c.getMethod("main", args.getClass());
        main.invoke(null, new Object[]{args});
    }

    private static ClassLoader getParentClassLoader() throws InvocationTargetException, IllegalAccessException {
        try {
            Method platformClassLoader = ClassLoader.class.getMethod("getPlatformClassLoader", null);
            return (ClassLoader)platformClassLoader.invoke(null, null);
        }
        catch (NoSuchMethodException noSuchMethodException) {
            return null;
        }
    }

    private static ManifestInfo getManifestInfo() throws IOException {
        Enumeration<URL> resEnum = Thread.currentThread().getContextClassLoader().getResources("META-INF/MANIFEST.MF");
        while (resEnum.hasMoreElements()) {
            try {
                URL url = resEnum.nextElement();
                InputStream is = url.openStream();
                if (is == null) continue;
                ManifestInfo result = new ManifestInfo();
                Manifest manifest = new Manifest(is);
                Attributes mainAttribs = manifest.getMainAttributes();
                result.rsrcMainClass = mainAttribs.getValue("Rsrc-Main-Class");
                String rsrcCP = mainAttribs.getValue("Rsrc-Class-Path");
                if (rsrcCP == null) {
                    rsrcCP = "";
                }
                result.rsrcClassPath = JarRsrcLoader.splitSpaces(rsrcCP);
                if (result.rsrcMainClass == null || result.rsrcMainClass.trim().isEmpty()) continue;
                return result;
            }
            catch (Exception exception) {}
        }
        System.err.println("Missing attributes for JarRsrcLoader in Manifest (Rsrc-Main-Class, Rsrc-Class-Path)");
        return null;
    }

    private static String[] splitSpaces(String line) {
        if (line == null) {
            return null;
        }
        ArrayList<String> result = new ArrayList<String>();
        int firstPos = 0;
        while (firstPos < line.length()) {
            int lastPos = line.indexOf(32, firstPos);
            if (lastPos == -1) {
                lastPos = line.length();
            }
            if (lastPos > firstPos) {
                result.add(line.substring(firstPos, lastPos));
            }
            firstPos = lastPos + 1;
        }
        return result.toArray(new String[result.size()]);
    }

    private static class ManifestInfo {
        String rsrcMainClass;
        String[] rsrcClassPath;

        private ManifestInfo() {
        }
    }
}
