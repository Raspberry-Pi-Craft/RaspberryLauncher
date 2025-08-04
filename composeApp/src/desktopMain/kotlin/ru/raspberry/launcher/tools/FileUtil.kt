package ru.raspberry.launcher.tools

import java.io.*
import java.math.BigInteger
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.attribute.BasicFileAttributeView
import java.nio.file.attribute.BasicFileAttributes
import java.security.DigestInputStream
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

class FileUtil {
    @Throws(IOException::class)
    fun getFile(archive: File, requestedFile: String?): ByteArray? {
        ByteArrayOutputStream().use { out ->
            ZipInputStream(FileInputStream(archive)).use { `in` ->
                while (true) {
                    var entry: ZipEntry?
                    do {
                        if ((`in`.getNextEntry().also { entry = it }) == null) {
                            return out.toByteArray()
                        }
                    } while (entry!!.getName() != requestedFile)

                    val buf = ByteArray(1024)

                    var len: Int
                    while ((`in`.read(buf).also { len = it }) > 0) {
                        out.write(buf, 0, len)
                    }
                }
            }
        }
    }

    companion object {
        const val DEFAULT_CHARSET: String = "UTF-8"

        val charset: Charset
            get() {
                try {
                    return Charset.forName(DEFAULT_CHARSET)
                } catch (var1: Exception) {
                    throw Error(DEFAULT_CHARSET + " is not supported", var1)
                }
            }

        @Throws(IOException::class)
        fun writeFile(file: File, text: String) {
            createFile(file)
            val os = BufferedOutputStream(FileOutputStream(file))
            val ow = OutputStreamWriter(os, StandardCharsets.UTF_8)
            ow.write(text)
            ow.close()
            os.close()
        }

        fun getFilename(path: String): String? {
            val folders: Array<String?> = path.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            val size = folders.size
            return if (size == 0) "" else folders[size - 1]
        }

        fun getDigest(file: File, algorithm: String, hashLength: Int): String? {
            var stream: DigestInputStream? = null

            try {
                stream = DigestInputStream(FileInputStream(file), MessageDigest.getInstance(algorithm))
                val ignored = ByteArray(65536)

                var read: Int
                do {
                    read = stream.read(ignored)
                } while (read > 0)

                return String.format(
                    Locale.ROOT,
                    "%1$0" + hashLength + "x",
                    BigInteger(1, stream.getMessageDigest().digest())
                )
            } catch (ignored: Exception) {
            } finally {
                Companion.close(stream!!)
            }

            return null
        }

        fun getSHA(file: File): String? {
            return getDigest(file, "SHA", 40)
        }

        @Throws(IOException::class)
        fun copyAndDigest(
            inputStream: InputStream,
            outputStream: OutputStream,
            algorithm: String,
            hashLength: Int
        ): String {
            val digest: MessageDigest
            try {
                digest = MessageDigest.getInstance(algorithm)
            } catch (var10: NoSuchAlgorithmException) {
                throw RuntimeException("Missing Digest. " + algorithm, var10)
            }

            val buffer = ByteArray(65536)

            try {
                var read = inputStream.read(buffer)
                while (read >= 1) {
                    digest.update(buffer, 0, read)
                    outputStream.write(buffer, 0, read)
                    read = inputStream.read(buffer)
                }
            } finally {
                close(inputStream)
                close(outputStream)
            }

            return String.format(Locale.ROOT, "%1$0" + hashLength + "x", BigInteger(1, digest.digest()))
        }

        @Throws(IOException::class)
        private fun createChecksum(file: File, algorithm: String): ByteArray {
            var fis: BufferedInputStream? = null
            try {
                fis = BufferedInputStream(FileInputStream(file))
                val e = ByteArray(1024)
                val complete = MessageDigest.getInstance(algorithm)
                var numRead: Int
                do {
                    numRead = fis.read(e)
                    if (numRead > 0) {
                        complete.update(e, 0, numRead)
                    }
                } while (numRead != -1)
                return complete.digest()
            } catch (e: NoSuchAlgorithmException) {
                throw Error(e)
            } finally {
                Companion.close(fis!!)
            }
        }

        @Throws(IOException::class)
        fun getChecksum0(file: File?, algorithm: String): String {
            if (!Objects.requireNonNull<File?>(file, "file").isFile()) {
                throw FileNotFoundException(file!!.getAbsolutePath())
            }
            val checksumBytes: ByteArray = Companion.createChecksum(file!!, algorithm)
            val checksumString = StringBuilder()
            for (b in checksumBytes) {
                checksumString.append(((b.toInt() and 255) + 256).toString(16).substring(1))
            }
            return checksumString.toString()
        }

        @Throws(IOException::class)
        fun getSha1(file: File?): String {
            return getChecksum0(file, "SHA-1")
        }

        fun getChecksum(file: File?, algorithm: String): String? {
            try {
                return getChecksum0(file, algorithm)
            } catch (ioE: IOException) {
                return null
            }
        }

        private fun close(a: Closeable) {
            try {
                a.close()
            } catch (var2: Exception) {
                var2.printStackTrace()
            }
        }

        val runningJar: File
            get() {
                try {
                    return File(
                        URLDecoder.decode(
                            FileUtil::class.java.getProtectionDomain().getCodeSource().getLocation().getPath(), "UTF-8"
                        )
                    )
                } catch (var1: UnsupportedEncodingException) {
                    throw RuntimeException("Cannot get running file!", var1)
                }
            }

        @Throws(IOException::class)
        fun copyFile(source: File, dest: File, replace: Boolean) {
            if (dest.isFile()) {
                if (!replace) {
                    return
                }
            } else {
                createFile(dest)
            }

            BufferedInputStream(FileInputStream(source)).use { `is` ->
                BufferedOutputStream(FileOutputStream(dest)).use { os ->
                    val buffer = ByteArray(1024)
                    var length: Int
                    while ((`is`.read(buffer).also { length = it }) > 0) {
                        os.write(buffer, 0, length)
                    }
                }
            }
        }

        fun deleteFile(file: File) {
            if (!file.isFile() && !file.isDirectory()) {
                return
            }

            val path = file.getAbsolutePath()

            if (file.delete()) {
                val parent = file.getParentFile()
                if (parent != null && parent != file) {
                    val list = parent.listFiles()
                    if (list != null && list.size <= 0) {
                        deleteFile(parent)
                    }
                }
            } else {
                if (fileExists(file)) {
                    println("Could not delete file: \${path}")
                }
            }
        }

        fun deleteDirectory(dir: File) {
            require(dir.isDirectory()) { "Specified path is not a directory: " + dir.getAbsolutePath() }

            val list = dir.listFiles()

            if (list == null) {
                throw RuntimeException("Folder is corrupted: " + dir.getAbsolutePath())
            }

            for (file in list) {
                if (file == dir) {
                    continue
                }

                if (file.isDirectory()) {
                    deleteDirectory(file)
                }

                if (file.isFile()) {
                    deleteFile(file)
                }
            }

            deleteFile(dir)
        }

        @Throws(IOException::class)
        fun getMd5(file: File?): String {
            return getChecksum0(file, "MD5")
        }

        @Throws(IOException::class)
        fun createFolder(dir: File?): Boolean {
            return if (dir == null) {
                throw NullPointerException()
            } else if (dir.isDirectory() && dir.exists()) {
                false
            } else if (!dir.mkdirs()) {
                throw IOException("Cannot create folders: " + dir.absolutePath)
            } else if (!dir.canWrite()) {
                throw IOException("Created directory is not accessible: " + dir.absolutePath)
            } else {
                true
            }
        }

        fun folderExists(folder: File?): Boolean {
            return folder != null && folder.isDirectory() && folder.exists()
        }

        fun fileExists(file: File?): Boolean {
            return file != null && file.isFile() && file.exists()
        }

        fun fileExists(path: String?): Boolean {
            return path != null && fileExists(File(path))
        }

        @Throws(IOException::class)
        fun createFile(file: File) {
            if (fileExists(file)) {
                return
            }

            if (file.getParentFile() != null && !folderExists(file.getParentFile())) {
                if (!file.getParentFile().mkdirs()) {
                    throw IOException("Could not create parent:" + file.getAbsolutePath())
                }
            }

            if (!file.createNewFile() && !fileExists(file)) {
                throw IOException("Could not create file, or it was created/deleted simultaneously: " + file.getAbsolutePath())
            }
        }

        @Throws(IOException::class)
        fun createFile(file: String) {
            createFile(File(file))
        }

        @Throws(IOException::class)
        fun getResource(resource: URL, charset: String): String {
            val `is` = BufferedInputStream(resource.openStream())
            val reader = InputStreamReader(`is`, charset)
            val b = StringBuilder()

            while (reader.ready()) {
                b.append(reader.read().toChar())
            }

            reader.close()
            return b.toString()
        }

        @Throws(IOException::class)
        fun getResource(resource: URL): String {
            return getResource(resource, "UTF-8")
        }

        private fun getNeighborFile(file: File, filename: String): File {
            var parent = file.getParentFile()
            if (parent == null) {
                parent = File("/")
            }

            return File(parent, filename)
        }

        fun getNeighborFile(filename: String): File {
            return getNeighborFile(runningJar, filename)
        }

        fun getExtension(f: File): String? {
            if (!f.isFile() && f.isDirectory()) {
                return null
            } else {
                var ext = ""
                val s = f.getName()
                val i = s.lastIndexOf(46.toChar())
                if (i > 0 && i < s.length - 1) {
                    ext = s.substring(i + 1).lowercase()
                }

                return ext
            }
        }

        @Throws(IOException::class)
        fun getSize(file: File): Long {
            val path = file.toPath()
            val view = Files.getFileAttributeView<BasicFileAttributeView>(path, BasicFileAttributeView::class.java)
            val attributes: BasicFileAttributes
            try {
                attributes = view.readAttributes()
            } catch (ioE: IOException) {
                throw IOException("Couldn't real attributes of " + file.getAbsolutePath(), ioE)
            }
            return attributes.size()
        }
    }
}
