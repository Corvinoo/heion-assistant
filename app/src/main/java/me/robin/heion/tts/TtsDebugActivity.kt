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

package me.robin.heion.tts

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.robin.heion.databinding.ActivityTtsDebugBinding
import me.robin.heion.tts.model.ModelStore
import me.robin.heion.App
import me.robin.heion.tts.inference.KokoroEngine
import me.robin.heion.tts.inference.KokoroTokenizer
import java.io.File
import java.io.FileInputStream

class TtsDebugActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTtsDebugBinding
    private lateinit var modelStore: ModelStore
    private lateinit var kokoroEngine: KokoroEngine
    //todo: load testing text from outside
    private val italianText: String = """Quel ramo del lago di Como, che volge a mezzogiorno, tra due catene ininterrotte di monti, tutto a seni e a golfi, a seconda dello sporgere e del rientrare di quelli, vien, quasi a un tratto, a ristringersi, e a prender corso e figura di fiume, tra un promontorio a destra, e un'ampia costiera dall'altra parte; 
        |e il ponte, che ivi congiunge le due rive, par che renda ancor più sensibile all'occhio questa trasformazione, e segni il punto in cui il lago cessa, e l'Adda rincomincia, per ripigliar poi nome di lago dove le rive, allontanandosi di nuovo, lascian l'acqua distendersi e rallentarsi in nuovi golfi e in nuovi seni.
        |La costiera, formata dal deposito di tre grandi torrenti, scende appoggiata ai due monti contigui, l'uno detto di san Martino, l'altro, con voce lombarda, il Resegone, dai molti suoi ricchiatemi, che in vero lo fanno somigliare a una sega: talché non è chi, al primo vederlo, purché sia di fronte, come per esempio di su le mura di Milano che guardano a settentrione, non lo discerna tosto, a un tal contrassegno, in quella lunga e vasta giogaia, dagli altri monti di nome più oscuro e di forma più comune.
        |Per un buon pezzo, la costa sale con un pendio lento e continuo; poi si rompe in poggi e in valloncelli, in erte e in ispianate, secondo l'ossatura de' due monti, e il lavoro dell'acque.Il lembo estremo, tagliato dalle foci de' torrenti, è quasi tutto ghiaia e ciottoloni; il resto, campi e vigne, sparse di terre, di ville, di casali; in qualche parte boschi, che si prolungano su per la montagna. Lecco, il principale di questi borghi, e che dà nome al territorio, giace poco discosto dal ponte, alla riva del lago, anzi viene in parte a trovarsi nel lago stesso, quando questo cresce; 
        |un gran borgo al giorno d'oggi, e che s'incammina a diventar città.""".trimMargin()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTtsDebugBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as App
        modelStore = app.appContainer.modelStore
        kokoroEngine = app.appContainer.kokoroEngine

        binding.btnRunCopy.setOnClickListener {
            runSmokeTest()
        }

        binding.btnTestEngine.setOnClickListener {
            testEngine()
        }

        binding.btnTestPhonemizer.setOnClickListener {
            testPhonemizer()
        }

        binding.btnTestLid.setOnClickListener {
            testLanguageDetection()
        }

        binding.btnTestFullPipeline.setOnClickListener {
            testFullPipeline()
        }

        binding.btnTestStreaming.setOnClickListener {
            testStreaming()
        }

        binding.btnTestEnglishOnly.setOnClickListener {
            testEnglishVoiceOnly()
        }

        binding.btnTestItNoChunk.setOnClickListener {
            testItalianNoChunk()
        }

        binding.btnTestItStreaming.setOnClickListener {
            testItalianStreaming()
        }

    }

    private fun testItalianNoChunk() {
        val app = application as App
        val ttsRouter = app.appContainer.ttsRouter
        log("Testing Italian (Full Text, No Chunking)...")
        lifecycleScope.launch {
            try {
                // We force 'it' to ensure the router picks the Italian phonemizer
                ttsRouter.say(italianText, "it")
                log("Full Italian text synthesis handoff completed.")
            } catch (e: Exception) {
                log("Italian No-Chunk test FAILED: ${e.message}")
            }
        }
    }

    private fun testItalianStreaming() {
        val app = application as App
        val streamingPlayer = app.appContainer.streamingTtsPlayer
        log("Testing Italian Streaming (Chunked)...")
        lifecycleScope.launch {
            try {
                // start() will auto-detect "it" from the first chunk
                streamingPlayer.start()
                
                // Simulate streaming tokens
                val tokens = italianText.split(" ")
                for (token in tokens) {
                    streamingPlayer.onToken("$token ")
                    delay(30) // Simulate generation speed
                }
                
                streamingPlayer.finish()
                log("Italian streaming test finalized.")
            } catch (e: Exception) {
                log("Italian Streaming test FAILED: ${e.message}")
            }
        }
    }

    private fun testStreaming() {
        val app = application as App
        val streamingPlayer = app.appContainer.streamingTtsPlayer

        log("Testing Streaming TTS Pipeline...")
        lifecycleScope.launch {
            try {
                log("1. Starting session (en)")
                streamingPlayer.start("en")

                val tokens = listOf(
                    "Hello ", "there! ", "This ", "is ", "a ", "test ", "of ", "the ", "streaming ", 
                    "TTS ", "system. ", "It ", "should ", "start ", "speaking ", "as ", "soon ", 
                    "as ", "the ", "first ", "sentence ", "is ", "complete. ", 
                    "Even ", "without ", "punctuation, ", "it ", "should ", "eventually ", 
                    "flush ", "due ", "to ", "inactivity"
                )

                for (token in tokens) {
                    log("Pushing token: \"$token\"")
                    streamingPlayer.onToken(token)
                    delay(100) // Simulate network delay
                }

                log("Tokens pushed. Waiting for inactivity flush...")
                delay(1000)
                
                log("Finalizing stream.")
                streamingPlayer.finish()
                
            } catch (e: Exception) {
                log("Streaming test FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun testAndSaveWav() {
        val app = application as App
        val engine = app.appContainer.kokoroEngine
        val phonemizer = app.appContainer.phonemizer
        val voiceRegistry = app.appContainer.voiceRegistry
        val player = app.appContainer.audioPlayer

        log("Testing English Voice & Saving to WAV...")
        lifecycleScope.launch {
            try {
                val text = "Hello! This is a test of the English Kokoro voice, being saved to a WAV file."
                val lang = "en"

                log("1. Phonemizing...")
                val phonemeResult = phonemizer.phonemize(text, lang)
                log("Phonemes: \"${phonemeResult.phonemes}\"")

                log("2. Loading Voice Style...")
                val voiceName = voiceRegistry.getVoiceForLanguage(lang)

                val tokens = KokoroTokenizer.tokenize(phonemeResult.phonemes)
                val styleVector = voiceRegistry.getStyleVector(voiceName, tokens.size)

                log("3. Inference...")
                val result = engine.synthesize(phonemeResult.phonemes, styleVector)

                log("4. Saving to WAV...")
                val outFile = File(getExternalFilesDir(null), "kokoro_test.wav")
                engine.saveToWav(result, outFile)
                log("WAV saved to: ${outFile.absolutePath}")
                log("You can pull it with: adb pull ${outFile.absolutePath}")

                log("5. Playing back...")
                player.play(result.audioData, result.sampleRate)

            } catch (e: Exception) {
                log("Test & Save FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun testEnglishVoiceOnly() {
        val app = application as App
        val ttsRouter = app.appContainer.ttsRouter

        log("Testing English Voice ONLY (Forced 'en')...")
        lifecycleScope.launch {
            try {
                val text = "Hello! This is a direct test of the English Kokoro voice."
                log("Processing: \"$text\"")
                
                ttsRouter.say(text, "en")
                log("Handoff to TtsRouter completed.")
            } catch (e: Exception) {
                log("English voice test FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun testLanguageDetection() {
        val app = application as App
        val languageDetector = app.appContainer.languageDetector

        log("Testing Language Identification (LID)...")
        lifecycleScope.launch {
            try {
                val testTexts = listOf(
                    "Hello, this is a test of the English voice.",
                    "Ciao, questo è un test della voce italiana.",
                    "こんにちは、これは日本語の声のテストです。"
                )

                for (text in testTexts) {
                    val guess = languageDetector.detect(text)
                    log("Input: \"$text\"")
                    log("Detected: ${guess?.languageTag} (Confidence: ${guess?.confidence})")
                }
            } catch (e: Exception) {
                log("LID test FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun testFullPipeline() {
        val app = application as App
        val ttsRouter = app.appContainer.ttsRouter
        val languageDetector = app.appContainer.languageDetector

        log("Testing Full TTS Pipeline (LID + Router)...")
        lifecycleScope.launch {
            try {
                val testTexts = listOf(
                    "Hello, this is a test of the English voice.",
//                    "Ciao, questo è un test della voce italiana.",
//                    "こんにちは、これは日本語の声のテストです。"
                )

                for (text in testTexts) {
                    log("\nProcessing: \"$text\"")
                    val guess = languageDetector.detect(text)
                    log("Detected language: ${guess?.languageTag} (Confidence: ${guess?.confidence})")
                    
                    log("Handing off to TtsRouter...")
                    ttsRouter.say(text, guess?.languageTag)
                    log("Request completed or cancelled for: \"$text\"")
                }
            } catch (e: Exception) {
                log("Pipeline test FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun testPhonemizer() {
        val app = application as App
        val phonemizer = app.appContainer.phonemizer
        val dataStore = app.appContainer.phonemeDataStore

        log("Testing Phonemizer...")
        lifecycleScope.launch {
            try {
                log("Ensuring phoneme data files...")
                dataStore.ensureDataFiles()

                log("\nTesting English (espeak-ng):")
                val enResult = phonemizer.phonemize("Hello, how are you?", "en")
                log("Input: \"Hello, how are you?\"")
                log("Result: ${enResult.phonemes}")

                log("\nTesting Japanese (OpenJTalk):")
                val jaResult = phonemizer.phonemize("こんにちは、お元気ですか？", "ja")
                log("Input: \"こんにちは、お元気ですか？\"")
                log("Result: ${jaResult.phonemes}")

            } catch (e: Exception) {
                log("Phonemizer test FAILED: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun testEngine() {
        log("Testing KokoroEngine initialization...")
        lifecycleScope.launch {
            try {
                log("Ensuring model files are present...")
                modelStore.ensureModelFiles()
                
                log("Attempting to load ONNX session...")
                kokoroEngine.load()
                log("Engine session loaded successfully!")
            } catch (e: Exception) {
                log("Engine test FAILED: ${e.message}")
            }
        }
    }

    private fun runSmokeTest() {
        binding.txtLog.text = "Starting smoke test...\n"
        lifecycleScope.launch {
            val paths = modelStore.ensureModelFiles()
            log("Copy finished. Files found: ${paths.size}")
            
            for (path in paths) {
                val file = File(path)
                val exists = file.exists()
                val size = if (exists) file.length() else -1
                log("File: ${file.name}\n  - Path: $path\n  - Exists: $exists\n  - Size: $size bytes")
                
                if (exists) {
                    try {
                        FileInputStream(file).use { input ->
                            val byte = input.read()
                            log("  - Verification: Successfully opened and read first byte: $byte")
                        }
                    } catch (e: Exception) {
                        log("  - Verification FAILED: ${e.message}")
                    }
                }
            }
        }
    }

    private fun log(message: String) {
        binding.txtLog.append("$message\n\n")
    }
}
