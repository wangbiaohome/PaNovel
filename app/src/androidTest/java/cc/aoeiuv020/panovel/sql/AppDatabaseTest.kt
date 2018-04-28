package cc.aoeiuv020.panovel.sql

import android.content.Context
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import cc.aoeiuv020.panovel.local.BookListData
import cc.aoeiuv020.panovel.util.assetsRead
import com.google.gson.Gson
import org.jetbrains.anko.AnkoLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.FixMethodOrder
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.MethodSorters

/**
 *
 * Created by AoEiuV020 on 2018.04.26-18:01:24.
 */
@RunWith(AndroidJUnit4::class)
@FixMethodOrder(value = MethodSorters.NAME_ASCENDING)
class AppDatabaseTest : AnkoLogger {

    @Rule
    @JvmField
    val watcher = TestUtil.timeWatcher

    lateinit var context: Context

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getTargetContext()
        DataManager.init(context)
    }

    private fun getNovelMiniCount(): Int {
        return DataManager.app.novelMiniDao().getNovelMiniCount()
    }

    private fun readBookListData(name: String): BookListData {
        val json = InstrumentationRegistry.getContext().assetsRead(name)
        return Gson().fromJson(json, BookListData::class.java)
    }

    private fun importBookListFromAssetsJson(name: String) {
        val bookListData = readBookListData(name)
        val bookList = DataManager.importBookList(bookListData)
        bookList.apply {
            assertTrue(id != null)
            assertEquals(bookListData.name, bookList.name)
        }
    }

    @Test
    fun a0_remove_all() {
        DataManager.app.clearAllTables()
    }

    @Test
    fun a1_import_test_book_list() {
        importBookListFromAssetsJson("bookList-test.json")
        val count = getNovelMiniCount()
        assertEquals(7, count)
    }

    @Test
    fun a2_import_backup_book_list() {
        importBookListFromAssetsJson("bookList-backup.json")
        val count = getNovelMiniCount()
        assertEquals(14, count)
    }

    @Test
    fun a3_import_again() {
        importBookListFromAssetsJson("bookList-test.json")
        val count = getNovelMiniCount()
        assertEquals(14, count)
    }

    @Test
    fun a4_query_first() {
        val bookLists = DataManager.getAllBookLists()
        val bookList = bookLists.first()
        assertEquals("测试书架", bookList.name)

        val novelMiniList = DataManager.getNovelMiniInBookList(bookList)
        val bookListData = readBookListData("bookList-test.json")
        val expected = bookListData.list.map { it.requester.extra }
                .sorted()
        val actual = novelMiniList.map { it.detailRequesterExtra!! }
                .sorted()
        assertEquals(expected, actual)
    }

    @Test
    fun a5_rename_first() {
        val bookList = DataManager.getAllBookLists().first()
        assertEquals("测试书架", bookList.name)
        val newName = "新名字书架"
        DataManager.renameBookList(bookList, newName)
        val result = DataManager.app.bookListDao()
                .getBookListById(bookList.id!!)!!
        assertEquals(newName, result.name)
    }

    @Test
    fun a6_remove_second() {
        val bookList = DataManager.getAllBookLists()[1]
        assertEquals("书架备份", bookList.name)
        DataManager.removeBookList(bookList)
        val count = getNovelMiniCount()
        // TODO: 同步删除小说，这里应该7本，
        assertEquals(14, count)
    }

    @Test
    fun a7_import_backup_book_list() {
        importBookListFromAssetsJson("bookList-backup.json")
        val count = getNovelMiniCount()
        assertEquals(14, count)
    }

    @Test
    fun az_remove_all() {
        DataManager.app.clearAllTables()
    }

    @Test
    fun b1_import_test_book_list() {
        importBookListFromAssetsJson("bookList-test.json")
        val count = getNovelMiniCount()
        assertEquals(7, count)
    }

    @Test
    fun b2_put_bookshelf() {
        val novelMiniList = DataManager.getAllBookLists()
                .first().id!!.let {
            DataManager.app.bookListDao()
                    .getNovelMiniInBookList(it)
        }
        val novelMini = novelMiniList.first()

        DataManager.putBookshelf(novelMini)
    }

    @Test
    fun b3_get_bookshelf() {
        val novelMiniList = DataManager.getAllBookshelf()
        assertEquals(1, novelMiniList.size)
        DataManager.getNovelMiniInBookList(
                DataManager.getAllBookLists().first()
        ).first().let { expected ->
            val actual = novelMiniList.first()
            assertEquals(expected.id, actual.id)
            assertEquals(expected.detailRequesterType, actual.detailRequesterType)
            assertEquals(expected.detailRequesterExtra, actual.detailRequesterExtra)
            assertEquals(expected.bookshelf, actual.bookshelf)
            assertEquals(true, actual.bookshelf)
            assertEquals(expected.chapterReadAt, actual.chapterReadAt)
        }
    }

    @Test
    fun b4_import_backup_book_list() {
        importBookListFromAssetsJson("bookList-backup.json")
        val count = getNovelMiniCount()
        assertEquals(14, count)
    }

    @Test
    fun b5_put_bookshelf_by_book_list() {
        val bookList = DataManager.getAllBookLists()
                .first { "书架备份" == it.name }
        DataManager.putBookshelf(bookList)
        val novelMiniList = DataManager.getAllBookshelf()
        assertEquals(8, novelMiniList.size)
    }

    @Test
    fun b6_remove_bookshelf_by_book_list() {
        DataManager.getAllBookLists()
                .first { "书架备份" == it.name }
                .let {
                    DataManager.removeBookshelf(it)
                }
        assertEquals(0, DataManager.getAllBookshelf().size)

        DataManager.getAllBookLists().forEach {
            DataManager.putBookshelf(it)
        }
        assertEquals(14, DataManager.getAllBookshelf().size)

        DataManager.getAllBookLists()
                .first { "书架备份" == it.name }
                .let {
                    DataManager.removeBookshelf(it)
                }
        assertEquals(6, DataManager.getAllBookshelf().size)
    }

    @Test
    fun z_remove_all() {
        DataManager.app.clearAllTables()
    }
}