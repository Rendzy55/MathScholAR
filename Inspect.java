import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.io.File;

public class Inspect {
    public static void main(String[] args) throws Exception {
        File f = new File("extracted/classes.jar");
        URL[] urls = { f.toURI().toURL() };
        URLClassLoader cl = new URLClassLoader(urls);
        Class<?> cls = cl.loadClass("io.github.sceneview.environment.HDRLoaderKt");
        for (Method m : cls.getDeclaredMethods()) {
            System.out.println(m.toString());
        }
    }
}
