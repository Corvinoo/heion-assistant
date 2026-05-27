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
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import me.robin.heion.R
import me.robin.heion.assistant.profiles.ProfileManager
import me.robin.heion.assistant.profiles.ProfileRegistry
import kotlin.math.abs

class ProfileChipView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val label: TextView
    private var profileManager: ProfileManager? = null
    private var viewScope: CoroutineScope? = null

    init {
        View.inflate(context, R.layout.view_profile_chip, this)
        label = findViewById(R.id.profileChipLabel)
        
        setupGestureDetector()
    }

    fun setProfileManager(pm: ProfileManager) {
        this.profileManager = pm
        
        // Cancel old scope if any
        viewScope?.cancel()
        viewScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        
        viewScope?.launch {
            pm.pendingProfile.collectLatest { profile ->
                profile?.let { setProfileName(it.displayName) }
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        viewScope?.cancel()
    }

    private fun setupGestureDetector() {
        val detector = GestureDetector(context, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(
                e1: MotionEvent?,
                e2: MotionEvent,
                velocityX: Float,
                velocityY: Float
            ): Boolean {
                if (e1 == null) return false
                val deltaX = e2.x - e1.x
                if (abs(deltaX) > 100 && abs(velocityX) > 100) {
                    if (deltaX > 0) {
                        onSwipeRight()
                    } else {
                        onSwipeLeft()
                    }
                    return true
                }
                return false
            }

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                onTap()
                return true
            }
        })

        setOnTouchListener { _, event ->
            detector.onTouchEvent(event)
            true
        }
    }

    private fun onSwipeLeft() {
        val pm = profileManager ?: return
        val current = pm.pendingProfile.value ?: return
        val all = ProfileRegistry.getAll()
        val index = all.indexOfFirst { it.id == current.id }
        if (index != -1) {
            val nextIndex = (index + 1) % all.size
            val nextProfile = all[nextIndex]
            viewScope?.launch {
                pm.switchTo(nextProfile.id)
            }
        }
    }

    private fun onSwipeRight() {
        val pm = profileManager ?: return
        val current = pm.pendingProfile.value ?: return
        val all = ProfileRegistry.getAll()
        val index = all.indexOfFirst { it.id == current.id }
        if (index != -1) {
            val prevIndex = if (index == 0) all.size - 1 else index - 1
            val prevProfile = all[prevIndex]
            viewScope?.launch {
                pm.switchTo(prevProfile.id)
            }
        }
    }

    private fun onTap() {
        // todo: Open profile picker bottom sheet || reconsidering design
    }
    
    fun setProfileName(name: String) {
        label.text = name
    }
}
