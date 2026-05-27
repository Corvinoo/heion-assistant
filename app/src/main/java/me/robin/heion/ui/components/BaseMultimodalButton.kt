/*
 *  Copyright (C) 2026 Corvinoo
 *  This file is part of Heion Cloudless Assistant
 *
 * Heion Cloudless Assistant is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * Heion Cloudless Assistant is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Heion Cloudless Assistant. If not, see <https://www.gnu.org/licenses/>.
 *
 * This program is subject to additional terms, experimental software disclaimers,
 * and trademark limitations pursuant to Section 7 of the GNU GPLv3.
 * See the README and first-launch notice for details.
 */

package me.robin.heion.ui.components

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.toColorInt
import me.robin.heion.R

abstract class BaseMultimodalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    protected val icon: ImageView
    protected val dotIndicator: ImageView

    protected var mIsPrimaryMode = true
    protected var mIsSelectedState = false
    protected var mIsLocked = false
    
    protected var primaryIconRes: Int = android.R.drawable.ic_menu_camera
    protected var secondaryIconRes: Int = android.R.drawable.ic_btn_speak_now

    init {
        inflate(context, R.layout.view_multimodal_button, this)
        icon = findViewById(R.id.buttonIcon)
        dotIndicator = findViewById(R.id.dotIndicator)
    }

    fun setIcons(primary: Int, secondary: Int) {
        primaryIconRes = primary
        secondaryIconRes = secondary
        updateUI()
    }

    protected fun setMode(primary: Boolean) {
        if (mIsPrimaryMode == primary) return
        mIsPrimaryMode = primary
        updateUI()
        onModeChanged(primary)
    }

    protected open fun onModeChanged(isPrimary: Boolean) {}

    fun isPrimaryMode(): Boolean = mIsPrimaryMode

    fun setSelectedState(selected: Boolean) {
        mIsSelectedState = selected
        updateUI()
    }

    fun setLocked(locked: Boolean) {
        mIsLocked = locked
    }

    fun isLocked(): Boolean = mIsLocked

    protected open fun updateUI() {
        icon.setImageResource(if (mIsPrimaryMode) primaryIconRes else secondaryIconRes)

        val tintColor = if (mIsSelectedState) {
            "#D9A5F7".toColorInt()
        } else {
            "#D0D0D8".toColorInt()
        }
        icon.imageTintList = android.content.res.ColorStateList.valueOf(tintColor)
        
        // Move dot indicator
        val params = dotIndicator.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (mIsPrimaryMode) {
            android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
        } else {
            android.view.Gravity.TOP or android.view.Gravity.CENTER_HORIZONTAL
        }
        dotIndicator.layoutParams = params
    }
}
