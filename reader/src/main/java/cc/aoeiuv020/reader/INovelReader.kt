package cc.aoeiuv020.reader

import android.content.Context

/**
 *
 * Created by AoEiuV020 on 2017.12.01-02:13:39.
 */
interface INovelReader {
    val ctx: Context

    var novel: Novel

    var chapterChangeListener: ChapterChangeListener?
    var menuListener: MenuListener?

    var requester: TextRequester
    var chapterList: List<Chapter>

    var currentChapter: Int
    var textProgress: Int
    val maxTextProgress: Int

    val config: ReaderConfig

    fun refreshCurrentChapter()

    fun scrollNext(): Boolean
    fun scrollPrev(): Boolean

    fun onDestroy()
}

abstract class BaseNovelReader(override var novel: Novel, override var requester: TextRequester) : INovelReader {
    override var chapterChangeListener: ChapterChangeListener? = null
    override var menuListener: MenuListener? = null
    override var chapterList: List<Chapter> = emptyList()
    override fun scrollNext(): Boolean = false
    override fun scrollPrev(): Boolean = false
}

