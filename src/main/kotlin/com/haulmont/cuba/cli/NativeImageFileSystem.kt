/*
 * Copyright (c) 2008-2018 Haulmont.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.haulmont.cuba.cli

import java.io.ByteArrayInputStream
import java.io.IOException
import java.net.URI
import java.net.URL
import java.nio.ByteBuffer
import java.nio.channels.Channels
import java.nio.channels.SeekableByteChannel
import java.nio.file.*
import java.nio.file.attribute.*
import java.nio.file.spi.FileSystemProvider
import java.util.regex.Pattern

class NativeImageFileSystem(private val provider: NativeImageFileSystemProvider) : FileSystem() {

    override fun getFileStores(): MutableIterable<FileStore> = arrayListOf()

    override fun getSeparator(): String = "/"

    override fun newWatchService(): WatchService = unsupported()

    override fun supportedFileAttributeViews(): MutableSet<String> = mutableSetOf()

    override fun isReadOnly(): Boolean = true

    override fun getPath(first: String, vararg more: String): Path =
            if (more.isNotEmpty())
                unsupported()
            else NativeImagePath(this, first)

    override fun provider(): FileSystemProvider = provider

    override fun isOpen(): Boolean = true

    override fun getUserPrincipalLookupService(): UserPrincipalLookupService = unsupported()

    override fun close() = throw UnsupportedOperationException()

    override fun getPathMatcher(syntaxAndPattern: String): PathMatcher {
        val pos = syntaxAndPattern.indexOf(':')
        if (pos <= 0 || pos == syntaxAndPattern.length)
            throw IllegalArgumentException()
        val syntax = syntaxAndPattern.substring(0, pos)
        val input = syntaxAndPattern.substring(pos + 1)

        val expr: String
        if (syntax.equals("glob", ignoreCase = true)) {
            throw UnsupportedOperationException()
        } else {
            if (syntax.equals("regex", ignoreCase = true)) {
                expr = input
            } else {
                throw UnsupportedOperationException("Syntax '" + syntax +
                        "' not recognized")
            }
        }

        val pattern = Pattern.compile(expr)

        return PathMatcher { path -> pattern.matcher(path.toString()).matches() }
    }

    override fun getRootDirectories(): MutableIterable<Path> = arrayListOf(NativeImagePath(this, "/"))

    override fun toString(): String = "resources:/"

    internal val root: Path = NativeImagePath(this, "/")

}

class NativeImagePath(private val fs: NativeImageFileSystem, internal val path: String) : Path {
    val resource: URL by lazy {
        javaClass.getResource(path)
    }
    val size: Long by lazy {
        resource.openStream().use { it.readAllBytes().size.toLong() }
    }

    override fun isAbsolute(): Boolean = true

    override fun getFileName(): Path = NativeImagePath(fs, path.split("/").last())

    override fun getName(index: Int): Path = NativeImagePath(fs, path.split("/")[0])

    override fun subpath(beginIndex: Int, endIndex: Int): Path = unsupported()

    override fun endsWith(other: Path?): Boolean = other is NativeImagePath && path.endsWith(other.path)

    override fun register(watcher: WatchService?, events: Array<out WatchEvent.Kind<*>>?, vararg modifiers: WatchEvent.Modifier?): WatchKey = unsupported()

    override fun relativize(other: Path?): Path = unsupported()

    override fun toUri(): URI = URI("resource:/$path")

    override fun toRealPath(vararg options: LinkOption?): Path = throw IOException()

    override fun normalize(): Path = this

    override fun getParent(): Path = NativeImagePath(fs, path.split("/").dropLast(1).joinToString("/"))

    override fun compareTo(other: Path?): Int = path.compareTo(other.toString())

    override fun getNameCount(): Int = path.count { it == '/' } - 1

    override fun startsWith(other: Path?): Boolean = path.startsWith(other.toString())

    override fun getFileSystem(): FileSystem = fs

    override fun getRoot(): Path = fs.root

    override fun resolve(other: Path?): Path = NativeImagePath(fs, path.dropLastWhile { it == '/' } + '/' + other.toString())

    override fun resolve(other: String): Path {
        return resolve(fileSystem.getPath(other))
    }

    override fun toAbsolutePath(): Path = this

    override fun toString(): String {
        return path
    }

    override fun equals(other: Any?): Boolean =
            other is NativeImagePath &&
                    other.toString() == path

    override fun hashCode(): Int {
        return path.hashCode()
    }
}

private fun unsupported(): Nothing {
    throw UnsupportedOperationException()
}

class NativeImageFileSystemProvider : FileSystemProvider() {
    private val fs: NativeImageFileSystem = NativeImageFileSystem(this)

    override fun checkAccess(path: Path?, vararg modes: AccessMode?) {
        val anyButRead = modes.any {
            it != AccessMode.READ
        }

        if (anyButRead)
            throw AccessDeniedException(path.toString())

        if (path.toString() !in index && "+$path" !in index)
            throw AccessDeniedException(path.toString())
    }

    override fun copy(source: Path?, target: Path?, vararg options: CopyOption?) = unsupported()

    override fun <V : FileAttributeView?> getFileAttributeView(path: Path?, type: Class<V>?, vararg options: LinkOption?): V {
        TODO("not implemented")
    }

    override fun isSameFile(path: Path?, path2: Path?): Boolean = path == path2

    override fun newFileSystem(uri: URI?, env: MutableMap<String, *>?): FileSystem = unsupported()

    override fun getScheme(): String = "resources:/"

    override fun isHidden(path: Path?): Boolean = false

    override fun newDirectoryStream(dir: Path, filter: DirectoryStream.Filter<in Path>?): DirectoryStream<Path> = object : DirectoryStream<Path> {
        private var closed = false

        override fun iterator(): MutableIterator<Path> {
            if (closed)
                throw ClosedDirectoryStreamException()

            val pathPrefix = dir.toString().removeSuffix("/") + "/"

            val numSlashes = pathPrefix.count { it == '/' }

            val itr = index.asSequence().filter { path ->
                val slashesInPath = path.count { it == '/' }
                (path.startsWith(pathPrefix)
                        || path.startsWith("+$pathPrefix"))
                        && (slashesInPath == numSlashes || (slashesInPath == numSlashes + 1 && path.endsWith("/")))
            }.map {
                NativeImagePath(fs, it.removePrefix("+"))
            }.filter {
                it.nameCount <= dir.nameCount + 1
            }.filter {
                filter?.accept(it) ?: true
            }.toMutableList().listIterator()

            return object : MutableIterator<Path> {
                override fun hasNext(): Boolean = !closed && itr.hasNext()

                override fun next(): Path {
                    if (closed)
                        throw NoSuchElementException()

                    return itr.next()
                }

                override fun remove() = itr.remove()
            }
        }

        override fun close() {
            closed = true
        }
    }

    override fun newByteChannel(path: Path?, options: MutableSet<out OpenOption>?, vararg attrs: FileAttribute<*>?): SeekableByteChannel {
        if (path !is NativeImagePath)
            unsupported()

        val buf = path.resource.readBytes()
        val rbc = Channels.newChannel(ByteArrayInputStream(buf))

        return object : SeekableByteChannel {

            var pos: Long = 0

            override fun isOpen(): Boolean = rbc.isOpen

            override fun position(): Long = pos

            override fun position(newPosition: Long): SeekableByteChannel = unsupported()

            override fun write(src: ByteBuffer?): Int = unsupported()

            override fun size(): Long = buf.size.toLong()

            override fun close() {
                rbc.close()
            }

            override fun truncate(size: Long): SeekableByteChannel = unsupported()

            override fun read(dst: ByteBuffer): Int {
                val n = rbc.read(dst)
                if (n > 0) {
                    this.pos += n.toLong()
                }

                return n
            }
        }
    }

    override fun delete(path: Path?) = unsupported()

    override fun <A : BasicFileAttributes?> readAttributes(path: Path?, type: Class<A>?, vararg options: LinkOption?): A {
        if (type == BasicFileAttributes::class.java || type == NativeFileAttributes::class) {
            return NativeFileAttributes(path as NativeImagePath) as A
        }

        unsupported()
    }

    override fun readAttributes(path: Path?, attributes: String?, vararg options: LinkOption?): MutableMap<String, Any> = unsupported()

    override fun getFileSystem(uri: URI?): FileSystem = fs

    override fun getPath(uri: URI?): Path = NativeImagePath(fs, uri.toString().replaceFirst(scheme, ""))

    override fun getFileStore(path: Path?): FileStore = unsupported()

    override fun setAttribute(path: Path?, attribute: String?, value: Any?, vararg options: LinkOption?) = unsupported()

    override fun move(source: Path?, target: Path?, vararg options: CopyOption?) = unsupported()

    override fun createDirectory(dir: Path?, vararg attrs: FileAttribute<*>?) = unsupported()
}

private val index: List<String> = try {
    NativeImageFileSystem::class.java.getResource("/index")?.openStream()?.reader()?.readLines()
} catch (e: Exception) {
    null
} ?: emptyList()

internal class NativeFileAttributes(private val path: NativeImagePath) : BasicFileAttributes {
    override fun isOther(): Boolean = false

    override fun isDirectory(): Boolean = path.isDirectory()

    override fun isSymbolicLink(): Boolean = false

    override fun isRegularFile(): Boolean = !isDirectory

    override fun creationTime(): FileTime = FileTime.fromMillis(0)

    override fun size(): Long = path.size

    override fun fileKey(): Any = path.path

    override fun lastModifiedTime(): FileTime = creationTime()

    override fun lastAccessTime(): FileTime = creationTime()
}

internal fun NativeImagePath.isDirectory() = "+$path" in index || "+$path".removeSuffix("/") in index
