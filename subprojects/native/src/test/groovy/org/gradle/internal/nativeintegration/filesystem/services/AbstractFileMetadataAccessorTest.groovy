/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.internal.nativeintegration.filesystem.services

import org.gradle.internal.file.FileMetadataSnapshot
import org.gradle.internal.file.FileType
import org.gradle.internal.nativeintegration.filesystem.FileMetadataAccessor
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition
import org.gradle.util.UsesNativeServices
import org.junit.Rule
import spock.lang.Specification

@UsesNativeServices
abstract class AbstractFileMetadataAccessorTest extends Specification {
    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider(getClass())
    abstract FileMetadataAccessor getAccessor()

    abstract boolean sameLastModified(FileMetadataSnapshot metadataSnapshot, File file)

    def "stats missing file"() {
        def file = tmpDir.file("missing")

        expect:
        def stat = accessor.stat(file)
        stat.type == FileType.Missing
        stat.lastModified == 0
        stat.length == 0
    }

    def "stats regular file"() {
        def file = tmpDir.file("file")
        file.text = "123"

        expect:
        def stat = accessor.stat(file)
        stat.type == FileType.RegularFile
        sameLastModified(stat, file)
        stat.length == 3
    }

    def "stats directory"() {
        def dir = tmpDir.file("dir").createDir()

        expect:
        def stat = accessor.stat(dir)
        stat.type == FileType.Directory
        stat.lastModified == 0
        stat.length == 0
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "stats symlink"() {
        def file = tmpDir.file("file")
        file.text = "123"
        def link = tmpDir.file("link")
        link.createLink(file)

        expect:
        def stat = accessor.stat(link)
        stat.type == FileType.RegularFile
        sameLastModified(stat, file)
        stat.length == 3
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "stats symlink to directory"() {
        def dir = tmpDir.createDir("dir")
        def link = tmpDir.file("link")
        link.createLink(dir)

        expect:
        def stat = accessor.stat(link)
        stat.type == FileType.Directory
        stat.lastModified == 0
        stat.length == 0
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "stats broken symlink"() {
        def file = tmpDir.file("file")
        def link = tmpDir.file("link")
        link.createLink(file)

        expect:
        def stat = accessor.stat(link)
        stat.type == FileType.Missing
        stat.lastModified == 0
        stat.length == 0
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "stats symlink pointing to symlink pointing to file"() {
        def file = tmpDir.file("file")
        file.text = "123"
        def link = tmpDir.file("link")
        link.createLink(file)
        def linkToLink = tmpDir.file("linkToLink")
        linkToLink.createLink(link)

        expect:
        def stat = accessor.stat(linkToLink)
        stat.type == FileType.RegularFile
        sameLastModified(stat, file)
        stat.length == 3
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "stats symlink pointing to broken symlink"() {
        def file = tmpDir.file("file")
        def link = tmpDir.file("link")
        link.createLink(file)
        def linkToLink = tmpDir.file("linkToBrokenLink")
        linkToLink.createLink(link)

        expect:
        def stat = accessor.stat(linkToLink)
        stat.type == FileType.Missing
        stat.lastModified == 0
        stat.length == 0
    }

    @Requires(TestPrecondition.SYMLINKS)
    def "stats a symlink cycle"() {
        def first = tmpDir.file("first")
        def second = tmpDir.file("second")
        def third = tmpDir.file("third")
        first.createLink(second)
        second.createLink(third)
        third.createLink(first)

        expect:
        def stat = accessor.stat(first)
        stat.type == FileType.Missing
        stat.lastModified == 0
        stat.length == 0
    }

    @Requires(TestPrecondition.UNIX_DERIVATIVE)
    def "stats named pipes"() {
        def pipe = tmpDir.file("testPipe").createNamedPipe()

        when:
        def stat = accessor.stat(pipe)
        then:
        stat.type == FileType.Missing
        stat.lastModified == 0
        stat.length == 0
    }
}
