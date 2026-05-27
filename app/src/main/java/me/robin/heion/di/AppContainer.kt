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

package me.robin.heion.di

import me.robin.heion.tts.model.KokoroVoiceRegistry
import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.robin.heion.assistant.agent.AgentLoop
import me.robin.heion.assistant.agent.ProfileResolver
import me.robin.heion.assistant.profiles.ProfileManager
import me.robin.heion.assistant.profiles.ProfileRegistry
import me.robin.heion.assistant.repository.ConversationRepository
import me.robin.heion.assistant.tools.ToolExecutor
import me.robin.heion.assistant.tools.ToolRegistry
import me.robin.heion.assistant.turns.TurnRunner
import me.robin.heion.assistant.voicesession.AssistContextRepository
import me.robin.heion.assistant.voicesession.QueryCancellationManager
import me.robin.heion.assistant.voicesession.VoiceRecorderController
import me.robin.heion.inference.ModelManager
import me.robin.heion.nlp.FastTextLanguageDetector
import me.robin.heion.nlp.LanguageDetector
import me.robin.heion.settings.SettingsRepository
import me.robin.heion.tts.TtsRouter
import me.robin.heion.tts.inference.KokoroEngine
import me.robin.heion.tts.model.ModelStore
import me.robin.heion.tts.phonemization.*
import me.robin.heion.tts.playback.AudioPlayer
import me.robin.heion.tts.streaming.StreamingTtsPlayer

class AppContainer(private val context: Context) {
    val modelManager = ModelManager
    val toolRegistry = ToolRegistry
    
    val settingsRepository = SettingsRepository(context)

    // NLP
    val languageDetector: LanguageDetector = FastTextLanguageDetector(context)

    // TTS Components
    val modelStore = ModelStore(context)
    val phonemeDataStore = PhonemeDataStore(context)
    val voiceRegistry = KokoroVoiceRegistry(modelStore)
    val audioPlayer = AudioPlayer(context)
    
    val espeakPhonemizer = EspeakPhonemizer(phonemeDataStore.espeakDataDir.parentFile!!)
    val openJTalkPhonemizer = OpenJTalkPhonemizer(
        phonemeDataStore.openJTalkDictDir,
        phonemeDataStore.openJTalkVoiceFile
    )

    val phonemizer = CompositePhonemizer(espeakPhonemizer, openJTalkPhonemizer)
    val kokoroEngine = KokoroEngine(modelStore)

    val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val ttsRouter = TtsRouter(
        voiceRegistry,
        phonemizer,
        kokoroEngine,
        audioPlayer
    )

    val streamingTtsPlayer = StreamingTtsPlayer(
        voiceRegistry,
        phonemizer,
        kokoroEngine,
        audioPlayer,
        languageDetector,
        serviceScope
    )
    
    val profileManager = ProfileManager(
        ProfileRegistry,
        context.getSharedPreferences("assistant_prefs", Context.MODE_PRIVATE)
    ).apply { restoreFromPrefs() }

    val conversationRepository = ConversationRepository()
    
    val profileResolver = ProfileResolver(profileManager)
    val turnRunner = TurnRunner(modelManager)
    val toolExecutor = ToolExecutor(toolRegistry)
    
    val agentLoop = AgentLoop(
        profileResolver,
        turnRunner,
        toolExecutor,
        toolRegistry
    )
    
    val recorderController = VoiceRecorderController(context)
    val contextRepository = AssistContextRepository()
    val cancellationManager = QueryCancellationManager()

    init {
        serviceScope.launch(Dispatchers.IO) {
            phonemeDataStore.ensureDataFiles()
            modelStore.ensureModelFiles()
        }
    }
}
