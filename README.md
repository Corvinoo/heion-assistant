# Heion: A Cloudless Private LLM Assistant

This Android application serves as a local and private assistant that can run multimodal LLMs.

---
You heard about it: the vast majority of online LLM providers can collect tons of user data, can train off user conversations, are subscription-based, and can be highly detrimental to communities and the environment because they run on colossal data centers. This pattern is increasingly common, and I think it's getting increasingly exhausting. 

Personally, I do not believe this is how AI should be. LLMs are a novel and interesting technology, capable of being genuinely useful, but only if used responsibly and sensibly.

This project tries to address that. This is a completely free, local and private assistant that aims to provide the utility of AI without giving up agency or control, all without exacerbating environmental complications.

Everything runs on your device. No processed data leaves your smartphone, and all conversations disappears when you close the chat. There is absolutely no telemetry or data collection.

Heion hopes to provide a small space of serenity for people who want to utilize AI as an interesting tool rather than a product, or just want to play around with models without having to indulge in consumerism.

***

## Features

The power and functionality of models that can run on limited mobile hardware have increased impressively in the last few years. This application functions mainly as a wrapper for Google's open-source Gemma 4 Edge SLM. Support is provided for both the E2B and E4B litert models.

I chose this model family due to their impressively small footprint (personal tests show E2B only takes approximately 2GB of RAM), good performance for their size (though definitely not exempt from hallucinations), and their tri-mode capabilities. This assistant supports audio and image input through the model.

If more efficient or updated models emerge in the future, I will consider implementing them.

### **How to Use:**
To **evoke** the assistant overlay you can press the "Trigger Assistant Overlay" button in the app or your smartphone's built-in "assistant shortcut" (e.g., long-pressing a specific button, depending on your device model and settings).

To **dismiss** the overlay you can press Home, Back on your phone navigation or tap any part of the *scrim* (background surrounding the chat container).

To **change assistant profile** you can swipe left and right on the profile name on top of the layout.

To **interrupt generation** you can press the "X" on the input bar or send a new prompt.

### Multimodal Implementation Details

*   **Conversation Mode:** When the assistant is invoked, you can speak anything. The application automatically forwards the audio to the model when you stop talking, and the model tries to understand it and respond. 
 
    If TTS is enabled and configured, the response will be spoken aloud. The assistant will start to listen again once the full message has been dictated.
    Pressing any button on the keyboard will disable audio mode. You can re-enable it by using your smartphone's assistant shortcut again.
> [!NOTE] 
> As of now TTS remains an experimental feature with limited working capabilities. Supported languages will expand in the future.

*   **Automatic TTS Language:** The TTS supports language routing using a LID system. If a language is detected that is not yet implemented, it will fall back to English dictation.
*   **Screen Mode:** When in text mode, you can press the "screen icon" located next to the text input. This forwards a screenshot of the content underlying the overlay; it allows you to ask for analysis or information about anything visible on the screen. 

    I plan to add the ability to attach photos from your gallery or directly from the camera.

  * **LLM Profiles:** from the overlay you can select various "modes" or "personas" for the AI. These use custom system prompts to give instructions to the model to answer in a specific manner.

    #### Some persona already bundled are:
    - *Default Assistant:* your "vanilla" assistant, does not have any specific instructions.
    - *Explain Like I'm Five (ELIA):* instructed to answer using very simple terms. It is configured to use **Thinking Mode**.
    - *Translator:* it will try to translate any text you send it to English.
    - *Dictionary:* it tries to define any word you send it and structures the answer like an online dictionary.
    - *Auto:* tries to understand the nature of your prompt and route it to what it thinks it's the best profile. *This profile is still WIP*.

## Future Plans

- **Retrieval Augmented Generation (RAG)** via a search engine to enhance model's current knowledge and grounding capabilities.
- **Sandbox code execution**, adding a safe python environment to the model to make it to solve mathematical problems and generate graphs.
- **Suit of commodity tools and functions**: a yet to decide pool of tools to augment general functionalities beyond simple text. 
####
> [!NOTE]
> The features mentioned above will be **optional** and **opt-in** by design, because using them might subject the input data to external privacy policies. 
>
>The application will continue to work privately and completely offline if decided to not use them.

- **UI/UX overhaul**: making the user interface and experience more modern and customizable.
- **Smart prompt routing**: most likely using a small cross-encoder or bi-encoder to better determine intent of the prompt, automatic persona selection and tool execution.
- **Making customizable personas**: adding the possibility to edit or create custom profiles.
***

## Hardware & Compatibility
*   **LLM Support:** Gemma 4 E2B and E4B litert are supported.

> [!WARNING]
> **The larger E4B model is not recommended due to size and potential instabilities.**
*   **Minimum Requirement:** Based on limited personal tests, 8GB of RAM and a processor with an NPU is the minimum hardware requirement to use E2B at an acceptable generation rate.
*   **Compatible Android Version:** Android 12+. Only Android 16 has been formally tested.
> [!NOTE]
> Performance may vary greatly depending on the smartphone model and Android version.


## Installation


After downloading the APK, you can download the necessary weights for the text inference and TTS directly from the application.


#### Optional Manual installation
If you prefer to manually load the models, for instance to debug more easily the application, you can load the files in the model library without needing to download them every time the apk is reinstalled.

[Gemma 4 E2B litert huggingface repository link](https://huggingface.co/litert-community/gemma-4-E2B-it-litert-lm) 

[Gemma 4 E4B litert huggingface repository link](https://huggingface.co/litert-community/gemma-4-E4B-it-litert-lm)

- **TTS:** (optional) The application will still work without the TTS, but to use it kokoro86M weights are required.
  
  List of models is available here on huggingface. Technically, any `.onxx` file should work, but the only tested quantization is [`model_q4f16.onnx`](https://huggingface.co/onnx-community/Kokoro-82M-ONNX/blob/306b5c63cce277355c5624553af3ab434baa13af/model_q4f16.onnx).
  
  Note: if you are building the application yourself, you can also directly place it inside the following directory: `app/main/assets/tts/kokoro`.



***

## Technical Stack

*   **TTS:** Powered by [Kokoro TTS](https://github.com/nazdridoy/kokoro-tts). I am not using the native Android TTS API because it solely serves as an interface for external services, and there is no guarantee that the text to synthesize won't be processed by an external API.
*   **Phonemization:** Handled by [espeak-ng](https://github.com/espeak-ng/espeak-ng). (Languages like Japanese will need a different phonemizer to work.)
*   **Language Identification:** Handled by [FastText](https://github.com/facebookresearch/fastText/).
 
All three libraries are native and utilize a Java Native Interface to work with Kotlin, which grants better overhead compared to higher-level wrappers.

*   **TTS runtime:** it uses [ONNX runtime](https://github.com/microsoft/onnxruntime) to run efficiently.
*   **Model Inference:** Handled by [Litert ML](https://github.com/google-ai-edge/litert).
*   **Markdown Rendering:** Handled by [noties/Markwon](https://github.com/noties/Markwon).

***

## Reason Behind the Name

Heion (平穏) from Japanese means "tranquil, quiet." I wish this project can bring a bit of peace into an otherwise chaotic landscape.

**Disclaimer:** This is a passion project. There are no guarantees or commitments to long-term support.


# **IMPORTANT AND LEGAL NOTES:** 
 By using this application, you acknowledge that:

  1. EXPERIMENTAL SOFTWARE & WARRANTY DISCLAIMER
  This software is provided "AS IS". This application is experimental and currently in development, may be unstable, and may behave unpredictably.
  To the maximum extent permitted by applicable law the developers assume no responsibility or liability for any damages, including any data loss, device malfunctions, hardware overheating, physical component damage, or financial losses by using this application.
  You are solely responsible for any consequences resulting from installation, execution, or reliance on the software or its outputs.

  2. TRADEMARK & NON-ASSOCIATION NOTICE
  All product names, logos, brands, and trademarks featured or referred to within this software and its documentation are the property of their respective trademark holders.
  These trademark holders are not affiliated with, nor do they sponsor, endorse, or approve this software or its creators.

  3. AI OUTPUT DISCLAIMER
  AI-generated content may be incorrect, incomplete, or inappropriate. 
  Outputs are not guaranteed and must not be relied upon without independent verification.
  The developers assume no responsibility for user-generated inputs or prompts, and any legal liability arising from the generation of harmful, illegal, or infringing content rests solely with the user.

  4. THIRD-PARTY TERMS OF CONDUCT
  This application allows you to interact with external model weights (e.g., Google's Gemma).
  You acknowledge that using such models may subject you to separate, independent terms of use and acceptable use policies set by their respective creators, which you are responsible for following.

  5. INTERNET CONNECTIVITY  
  This application can make use of an internet connection to automatically download necessary external dependencies required for its proper functionality. 
  Standard internet data rates, carrier fees, and usage charges from your internet service provider may apply during these downloads.
  Alternatively, an option to manually load all required dependencies in the software without downloading them from the application is also provided.

### Testing Credits
Testing utilities are not bundled with the APK. You can view the full third-party attribution list and license types in [ATTRIBUTION_TESTING.md](./ATTRIBUTION_TESTING.md).