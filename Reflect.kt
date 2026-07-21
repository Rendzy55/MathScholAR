import java.lang.reflect.Method
import java.net.URL
import java.net.URLClassLoader
import java.io.File

fun main() {
    val f = File("extracted/classes.jar")
    val urls = arrayOf(f.toURI().toURL())
    val cl = URLClassLoader(urls)
    try {
        val cls = cl.loadClass("io.github.sceneview.SceneView")
        cls.declaredMethods.forEach {
            println(it.name)
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
