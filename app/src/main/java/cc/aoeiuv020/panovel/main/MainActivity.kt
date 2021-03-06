@file:Suppress("DEPRECATION")

package cc.aoeiuv020.panovel.main

import android.app.ProgressDialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import cc.aoeiuv020.panovel.App
import cc.aoeiuv020.panovel.BuildConfig
import cc.aoeiuv020.panovel.R
import cc.aoeiuv020.panovel.booklist.BookListFragment
import cc.aoeiuv020.panovel.bookshelf.BookshelfFragment
import cc.aoeiuv020.panovel.bookstore.BookstoreActivity
import cc.aoeiuv020.panovel.donate.DonateActivity
import cc.aoeiuv020.panovel.history.HistoryFragment
import cc.aoeiuv020.panovel.local.Bookshelf
import cc.aoeiuv020.panovel.local.Check
import cc.aoeiuv020.panovel.local.DevMessage
import cc.aoeiuv020.panovel.local.Settings
import cc.aoeiuv020.panovel.open.OpenManager
import cc.aoeiuv020.panovel.search.FuzzySearchActivity
import cc.aoeiuv020.panovel.server.UpdateManager
import cc.aoeiuv020.panovel.server.common.md5
import cc.aoeiuv020.panovel.server.dal.model.autogen.Novel
import cc.aoeiuv020.panovel.server.jpush.JPushTagReceiver
import cc.aoeiuv020.panovel.server.jpush.TagAliasBean
import cc.aoeiuv020.panovel.server.jpush.TagAliasOperatorHelper
import cc.aoeiuv020.panovel.settings.SettingsActivity
import cc.aoeiuv020.panovel.util.asyncExecutor
import cc.aoeiuv020.panovel.util.show
import com.google.android.gms.ads.AdListener
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.dialog_editor.view.*
import net.lucode.hackware.magicindicator.ViewPagerHelper
import net.lucode.hackware.magicindicator.buildins.commonnavigator.CommonNavigator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.CommonNavigatorAdapter
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.abs.IPagerTitleView
import net.lucode.hackware.magicindicator.buildins.commonnavigator.indicators.LinePagerIndicator
import net.lucode.hackware.magicindicator.buildins.commonnavigator.titles.ColorTransitionPagerTitleView
import org.jetbrains.anko.*
import java.util.concurrent.atomic.AtomicInteger


/**
 *
 * Created by AoEiuV020 on 2017.10.15-15:53:19.
 */
class MainActivity : AppCompatActivity(), AnkoLogger {
    lateinit var progressDialog: ProgressDialog
    private lateinit var bookshelfFragment: BookshelfFragment
    private lateinit var historyFragment: HistoryFragment
    lateinit var bookListFragment: BookListFragment
    private var position: Int = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!BuildConfig.DEBUG) {
            Check.asyncCheckSignature(this)
        }

        Check.asyncCheckVersion(this)
        DevMessage.asyncShowMessage(this)

        setSupportActionBar(toolbar)

        JPushTagReceiver.register(this, tagReceiver)

        progressDialog = ProgressDialog(this)

        bookshelfFragment = BookshelfFragment()
        historyFragment = HistoryFragment()
        bookListFragment = BookListFragment()

        initTab(R.string.bookshelf to bookshelfFragment,
                R.string.book_list to bookListFragment,
                R.string.history to historyFragment)


        container.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                this@MainActivity.position = position
                // 切回书架时刷新一下，
                if (position == 0) {
                    bookshelfFragment.refresh()
                }
            }

        })

        fab.setOnClickListener { _ ->
            when (position) {
                1 -> bookListFragment.newBookList()
                else -> BookstoreActivity.start(this)
            }

        }

        ad_view.adListener = object : AdListener() {
            override fun onAdLoaded() {
                ad_view.show()
            }
        }

        if (Settings.adEnabled) {
            ad_view.loadAd(App.adRequest)
        }

        if (isTaskRoot) {
            // 只在第一个activity初始化这个负责上传更新的，
            UpdateManager.create(this)
        } else {
            // 避免多开，
            // 初始化完了再退出，否则可能崩溃，
            finish()
        }
    }

    private fun initTab(vararg pair: Pair<Int, Fragment>) {
        val (titleIdList, fragmentList) = pair.unzip()
        val pagerAdapter = object : FragmentPagerAdapter(supportFragmentManager) {
            override fun getItem(position: Int): Fragment = fragmentList[position]

            override fun getCount(): Int = fragmentList.size

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val fragment = super.instantiateItem(container, position)
                when (fragment) {
                    is BookshelfFragment -> bookshelfFragment = fragment
                    is BookListFragment -> bookListFragment = fragment
                    is HistoryFragment -> historyFragment = fragment
                }
                return fragment
            }

        }

        container.adapter = pagerAdapter

        val commonNavigator = CommonNavigator(this)
        val titleList = titleIdList.map {
            getString(it)
        }
        commonNavigator.adapter = object : CommonNavigatorAdapter() {

            override fun getCount(): Int = titleList.size

            override fun getTitleView(context: Context, index: Int): IPagerTitleView {
                val colorTransitionPagerTitleView = ColorTransitionPagerTitleView(context)
                colorTransitionPagerTitleView.normalColor = Color.GRAY
                colorTransitionPagerTitleView.selectedColor = Color.WHITE
                colorTransitionPagerTitleView.text = titleList[index]
                colorTransitionPagerTitleView.setOnClickListener { container.currentItem = index }
                return colorTransitionPagerTitleView
            }

            override fun getIndicator(context: Context): IPagerIndicator {
                val indicator = LinePagerIndicator(context)
                indicator.mode = LinePagerIndicator.MODE_EXACTLY
                indicator.setColors(Color.WHITE)
                return indicator
            }
        }
        magic_indicator.navigator = commonNavigator
        ViewPagerHelper.bind(magic_indicator, container)

    }

    override fun onPause() {
        ad_view.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        ad_view.resume()
    }

    override fun onDestroy() {
        JPushTagReceiver.unregister(this, tagReceiver)
        ad_view.destroy()
        if (isTaskRoot) {
            UpdateManager.destroy(this)
        }
        super.onDestroy()
    }

    private fun showExplain() {
        alert(assets.open("Explain.txt").reader().readText(), getString(R.string.explain)) {
            yesButton { }
        }.show()
    }

    private fun scan() {
        val intent = Intent("com.google.zxing.client.android.SCAN")
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE")
        try {
            startActivityForResult(intent, 0)
        } catch (_: ActivityNotFoundException) {
            toast("没安装zxing二维码扫描器，")
        } catch (_: SecurityException) {
            toast("没权限？这里是调用zxing扫码，")
        }
    }

    private fun open() {
        alert {
            titleResource = R.string.open
            val layout = View.inflate(this@MainActivity, R.layout.dialog_editor, null)
            customView = layout
            val etName = layout.editText
            yesButton {
                val url = etName.text.toString()
                if (url.isNotEmpty()) {
                    OpenManager.open(this@MainActivity, url)
                }
            }
        }.show()
    }

    private val sequence: AtomicInteger = AtomicInteger()
    private val tagReceiver: JPushTagReceiver = JPushTagReceiver.create { jPushMessage, tagAliasBean ->
        val message = "成功订阅当前书架<${jPushMessage.tags.size}>本，"
        info { message }
        toast(message)
    }

    private fun subscript() {
        // 初始化，其中有用到Handler，要在主线程初始化，
        TagAliasOperatorHelper.getInstance()
        asyncExecutor.execute {
            val bean = TagAliasBean()
            bean.action = TagAliasOperatorHelper.ACTION_SET
            bean.tags = Bookshelf.list().map {
                it.requester.run {
                    Novel().apply {
                        requesterType = type
                        requesterExtra = extra
                    }.md5()
                }
            }.toSet()
            TagAliasOperatorHelper.getInstance().handleAction(this, sequence.getAndIncrement(), bean)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        data?.extras?.getString("SCAN_RESULT")?.let {
            OpenManager.open(this, it)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.settings -> SettingsActivity.start(this)
            R.id.search -> FuzzySearchActivity.start(this)
            R.id.scan -> scan()
            R.id.open -> open()
            R.id.subscript -> subscript()
            R.id.donate -> DonateActivity.start(this)
            R.id.explain -> showExplain()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private val snack: Snackbar by lazy {
        Snackbar.make(fab, "", Snackbar.LENGTH_SHORT)
    }

    fun showMessage(message: String) {
        snack.setText(message)
        snack.show()
    }

    fun showError(message: String, e: Throwable) {
        progressDialog.dismiss()
        snack.setText(message + e.message)
        snack.show()
    }
}
