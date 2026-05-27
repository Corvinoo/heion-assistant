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
import android.view.HapticFeedbackConstants

class LongPressMultimodalButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BaseMultimodalButton(context, attrs, defStyleAttr) {

    var onModeChangedListener: ((Boolean) -> Unit)? = null

    init {
        setupGestures()
        updateUI()
    }

    private fun setupGestures() {
        setOnLongClickListener {
            if (!mIsLocked) {
                setMode(!mIsPrimaryMode)
                performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            }
            true
        }
        
        setOnClickListener {
            // Default click behavior is to perform normal click action
        }
    }

    override fun onModeChanged(isPrimary: Boolean) {
        onModeChangedListener?.invoke(isPrimary)
    }
}
