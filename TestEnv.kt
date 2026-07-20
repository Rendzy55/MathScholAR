import io.github.sceneview.SceneView
import io.github.sceneview.environment.loadEnvironmentAsync

fun test(sceneView: SceneView) {
    sceneView.loadEnvironmentAsync(
        hdrFileLocation = "skybox/sekolah.hdr"
    )
}
