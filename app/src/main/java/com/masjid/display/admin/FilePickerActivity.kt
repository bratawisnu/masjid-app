package com.masjid.display.admin

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContract
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import com.masjid.display.databinding.ActivityFilePickerBinding
import com.masjid.display.databinding.ItemFileRowBinding
import java.io.File
import java.util.Locale

/**
 * A file browser the caretaker can drive with a remote.
 *
 * Android's own picker (`ACTION_OPEN_DOCUMENT`, via DocumentsUI) is the obvious
 * choice and was what this app used first — but a great many Android TV boxes
 * ship without DocumentsUI, or hide it from Leanback. On those the picker never
 * opens: the system answers "You don't have an app that can do this" and the
 * caretaker is simply stuck, with no way to add an image or a video at all.
 *
 * So the app browses storage itself. That costs a read permission, which SAF
 * would not have needed, but a permission the caretaker can grant beats a
 * picker that cannot be summoned.
 */
class FilePickerActivity : AppCompatActivity() {

    companion object {
        private const val EXTRA_MODE = "mode"
        private const val EXTRA_PATH = "path"

        private const val PERMISSION_REQUEST = 1

        /** Extensions we can actually decode or play, lowercase, no dot. */
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "webm", "3gp", "m4v", "mov", "ts")

        private const val MARKER_UP = "⬑"
        private const val MARKER_FOLDER = "▸"
        private const val MARKER_FILE = "•"
    }

    enum class Mode { IMAGE, VIDEO }

    /**
     * Launches the browser and returns the chosen file's absolute path, or null
     * if the caretaker backed out.
     *
     * A path rather than a `content://` URI: the caller copies the file into
     * app storage straight away, and a plain path is all that copy needs.
     */
    class Contract : ActivityResultContract<Mode, String?>() {
        override fun createIntent(context: Context, input: Mode): Intent =
            Intent(context, FilePickerActivity::class.java).putExtra(EXTRA_MODE, input.name)

        override fun parseResult(resultCode: Int, intent: Intent?): String? =
            if (resultCode == Activity.RESULT_OK) intent?.getStringExtra(EXTRA_PATH) else null
    }

    private lateinit var binding: ActivityFilePickerBinding
    private lateinit var mode: Mode

    /** Null while the volume list is showing, which has no directory of its own. */
    private var currentDir: File? = null

    /**
     * Absolute paths of the volume roots, so navigation can tell "at the top of
     * a drive" from "inside a folder" — going up from a volume root belongs in
     * the volume list, not in the filesystem root above it.
     */
    private var volumeRoots: Set<String> = emptySet()
    private var entries: List<Entry> = emptyList()

    /**
     * One row: a folder to open, a file to choose, or the way back out.
     *
     * [label] is separate from the file name so a volume can be listed under a
     * name the caretaker recognises — "Penyimpanan internal" rather than the
     * numeric mount point the system gives a USB stick.
     *
     * On an [isUp] row a null [file] means there is no folder above this one,
     * so the way out is the volume list.
     */
    private data class Entry(
        val label: String,
        val file: File?,
        val isDirectory: Boolean,
        val isUp: Boolean = false
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilePickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mode = Mode.valueOf(intent.getStringExtra(EXTRA_MODE) ?: Mode.IMAGE.name)
        binding.textTitle.text = when (mode) {
            Mode.IMAGE -> "Pilih Gambar"
            Mode.VIDEO -> "Pilih Video"
        }

        binding.listFiles.adapter = FileAdapter()
        binding.listFiles.setOnItemClickListener { _, _, position, _ -> onEntryClicked(entries[position]) }

        onBackPressedDispatcher.addCallback(this) { goBack() }

        if (hasReadPermission()) {
            showVolumes()
        } else {
            requestPermissions(requiredPermissions(), PERMISSION_REQUEST)
        }
    }

    /**
     * The top level: every volume the box has, internal storage first.
     *
     * A USB stick is the usual way a file reaches a TV box, and it does not
     * live under the internal storage root — so starting inside internal
     * storage would hide the very drive the caretaker just plugged in.
     *
     * getExternalStorageDirectory is deprecated under scoped storage, but the
     * read permission this app holds is a *media* permission: photos and videos
     * stay readable by path on every volume, which is exactly and only what the
     * browser lists.
     */
    @Suppress("DEPRECATION")
    private fun showVolumes() {
        currentDir = null
        binding.textLocation.text = "Pilih penyimpanan"

        val internal = Environment.getExternalStorageDirectory()

        // getExternalFilesDirs returns the app's own folder on each volume;
        // climbing out of Android/data/<pkg>/files lands on that volume's root.
        val removable = getExternalFilesDirs(null)
            .filterNotNull()
            .mapNotNull { volumeRootOf(it) }
            .filter { it.absolutePath != internal.absolutePath && it.canRead() }
            .distinctBy { it.absolutePath }

        entries = buildList {
            if (internal.canRead()) add(Entry("Penyimpanan internal", internal, isDirectory = true))
            removable.forEach { add(Entry("USB / kartu (${it.name})", it, isDirectory = true)) }
        }
        volumeRoots = entries.mapNotNull { it.file?.absolutePath }.toSet()

        (binding.listFiles.adapter as FileAdapter).notifyDataSetChanged()

        if (entries.isEmpty()) {
            showMessage("Tidak ada penyimpanan yang bisa dibaca.")
        } else {
            binding.listFiles.isVisible = true
            binding.textEmpty.isVisible = false
            binding.listFiles.requestFocus()
        }
    }

    /**
     * Turns `<volume>/Android/data/<pkg>/files` back into `<volume>`, or null
     * if the path isn't shaped that way.
     */
    private fun volumeRootOf(appDir: File): File? =
        // files -> <pkg> -> data -> Android -> the volume itself.
        appDir.parentFile?.parentFile?.parentFile?.parentFile?.takeIf { it.isDirectory }

    private fun requiredPermissions(): Array<String> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Scoped per media type from 13 onwards; asking for the storage
            // permission there is silently refused.
            when (mode) {
                Mode.IMAGE -> arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
                Mode.VIDEO -> arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
            }
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

    private fun hasReadPermission(): Boolean = requiredPermissions().all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUEST) return

        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            showVolumes()
        } else {
            // Refusing leaves nothing to browse, so say what was refused and
            // where to turn it back on rather than showing an empty list.
            showMessage(
                "Aplikasi belum diizinkan membaca penyimpanan, jadi berkasnya tidak bisa " +
                    "ditampilkan. Buka Setelan > Aplikasi > Masjid Display > Izin untuk " +
                    "mengizinkannya, lalu coba lagi."
            )
        }
    }

    private fun showDirectory(dir: File) {
        val children = dir.listFiles()
        if (children == null) {
            showMessage("Folder ini tidak bisa dibaca. Tekan Kembali untuk memilih folder lain.")
            return
        }

        currentDir = dir
        binding.textLocation.text = dir.absolutePath

        val folders = children
            .filter { it.isDirectory && it.canRead() && !it.isHidden }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
            .map { Entry(it.name, it, isDirectory = true) }

        val files = children
            .filter { it.isFile && !it.isHidden && matchesMode(it) }
            .sortedBy { it.name.lowercase(Locale.getDefault()) }
            .map { Entry(it.name, it, isDirectory = false) }

        // Always offer the way out, even at a volume root — there it steps back
        // to the volume list rather than to the filesystem root, which the
        // caretaker has no business wandering into. A null file marks exactly
        // that case; see onEntryClicked.
        val parent = parentOf(dir)
        val up = listOf(
            Entry(
                label = if (parent == null) "Pilih penyimpanan" else "Kembali",
                file = parent,
                isDirectory = true,
                isUp = true
            )
        )

        entries = up + folders + files
        (binding.listFiles.adapter as FileAdapter).notifyDataSetChanged()

        // The list always holds the up row, so it stays on screen; the message
        // sits above it and tells the caretaker why the folder looks bare.
        val hasContent = folders.isNotEmpty() || files.isNotEmpty()
        binding.listFiles.isVisible = true
        binding.textEmpty.isVisible = !hasContent
        if (!hasContent) {
            binding.textEmpty.text = when (mode) {
                Mode.IMAGE -> "Tidak ada gambar di folder ini."
                Mode.VIDEO -> "Tidak ada video di folder ini."
            }
        }
        // Focus the list so the first press of the D-pad moves through it
        // instead of landing somewhere the caretaker can't see.
        binding.listFiles.requestFocus()
    }

    private fun showMessage(message: String) {
        binding.textEmpty.text = message
        binding.textEmpty.isVisible = true
        binding.listFiles.isVisible = false
    }

    private fun matchesMode(file: File): Boolean {
        val extension = file.extension.lowercase(Locale.getDefault())
        return when (mode) {
            Mode.IMAGE -> extension in IMAGE_EXTENSIONS
            Mode.VIDEO -> extension in VIDEO_EXTENSIONS
        }
    }

    /**
     * The folder one step up, or null when [dir] is a volume root and there is
     * nothing above it worth showing.
     */
    private fun parentOf(dir: File): File? {
        if (dir.absolutePath in volumeRoots) return null
        return dir.parentFile?.takeIf { it.canRead() }
    }

    private fun onEntryClicked(entry: Entry) {
        val file = entry.file
        if (file == null) {
            // Only the up row at a volume root carries no file.
            showVolumes()
        } else if (entry.isDirectory) {
            showDirectory(file)
        } else {
            setResult(RESULT_OK, Intent().putExtra(EXTRA_PATH, file.absolutePath))
            finish()
        }
    }

    /**
     * Back climbs one folder instead of closing, so the caretaker can wander
     * into a directory and get out again without starting over. From a volume
     * root it returns to the volume list, and only closes the picker from
     * there — one press of Back always undoes one step forward.
     */
    private fun goBack() {
        val dir = currentDir
        when {
            // A message is showing in place of the list — an unreadable folder
            // or drive, or a refused permission. Back restores whatever we were
            // looking at before, and only closes the picker when there is
            // nothing to go back to.
            !binding.listFiles.isVisible -> when {
                dir != null -> showDirectory(dir)
                volumeRoots.isNotEmpty() -> showVolumes()
                else -> finish()
            }
            dir == null -> finish()
            else -> parentOf(dir)?.let { showDirectory(it) } ?: showVolumes()
        }
    }

    private inner class FileAdapter : BaseAdapter() {
        override fun getCount(): Int = entries.size

        override fun getItem(position: Int): Any = entries[position]

        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val row = convertView?.let { ItemFileRowBinding.bind(it) }
                ?: ItemFileRowBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            val entry = entries[position]

            row.textMarker.text = when {
                entry.isUp -> MARKER_UP
                entry.isDirectory -> MARKER_FOLDER
                else -> MARKER_FILE
            }
            row.textName.text = entry.label
            // Size belongs to files alone; on a folder row it would be a
            // number the caretaker can't act on.
            row.textMeta.text = entry.file?.takeIf { !entry.isDirectory }?.let { readableSize(it) }.orEmpty()

            return row.root
        }

        private fun readableSize(file: File): String {
            val bytes = file.length()
            val mb = bytes / (1024f * 1024f)
            return if (mb < 1f) {
                "${(bytes / 1024f).toInt()} KB"
            } else {
                String.format(Locale.getDefault(), "%.1f MB", mb)
            }
        }
    }
}
