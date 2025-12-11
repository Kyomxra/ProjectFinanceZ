package com.example.projectmap2.ui.viewmodels

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.projectmap2.ui.models.CategoryPrediction
import com.example.projectmap2.ui.models.MLState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel

class MLViewModel : ViewModel() {

    private val _mlState = MutableStateFlow<MLState>(MLState.Idle)
    val mlState: StateFlow<MLState> = _mlState.asStateFlow()

    private var interpreter: Interpreter? = null
    private var wordIndex: Map<String, Int> = emptyMap()
    private val maxLength = 10

    private val categories = listOf(
        "Makan",
        "Transport",
        "Belanja",
        "Hiburan",
        "Lainnya"
    )

    fun initializeModel(context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Load TFLite model
                val modelBuffer = loadModelFile(context, "expense_model.tflite")
                interpreter = Interpreter(modelBuffer)

                // Load tokenizer config
                wordIndex = loadTokenizerConfig(context)

                Log.d("MLViewModel", "✅ Model initialized successfully")
            } catch (e: Exception) {
                Log.e("MLViewModel", "❌ Error initializing model: ${e.message}")
                withContext(Dispatchers.Main) {
                    _mlState.value = MLState.Error("Failed to load model: ${e.message}")
                }
            }
        }
    }

    fun predictCategory(text: String) {
        if (text.isEmpty() || text.length < 3) {
            _mlState.value = MLState.Idle
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _mlState.value = MLState.Loading
                }

                // Preprocess input
                val input = preprocessText(text)

                // Run inference (ASYNC - NO UI BLOCKING!)
                val output = Array(1) { FloatArray(5) }
                interpreter?.run(input, output)

                // Process results
                val predictions = output[0]
                val maxIndex = predictions.indices.maxByOrNull { predictions[it] } ?: 0
                val confidence = predictions[maxIndex]

                // Create prediction map
                val allPredictions = categories.indices.associate {
                    categories[it] to predictions[it]
                }

                val result = CategoryPrediction(
                    category = categories[maxIndex],
                    confidence = confidence,
                    allPredictions = allPredictions
                )

                withContext(Dispatchers.Main) {
                    _mlState.value = MLState.Success(result)
                }

                Log.d("MLViewModel", "Prediction: ${categories[maxIndex]} (${(confidence * 100).toInt()}%)")

            } catch (e: Exception) {
                Log.e("MLViewModel", "Prediction error: ${e.message}")
                withContext(Dispatchers.Main) {
                    _mlState.value = MLState.Error("Prediction failed: ${e.message}")
                }
            }
        }
    }

    private fun preprocessText(text: String): Array<FloatArray> {
        // Tokenization: Convert text to sequence of integers
        val words = text.lowercase().split(" ")
        val sequence = mutableListOf<Int>()

        for (word in words) {
            val index = wordIndex[word] ?: 1 // 1 = OOV token
            if (index < 1000) { // max_words limit
                sequence.add(index)
            }
        }

        // Padding/Truncating to maxLength
        val padded = FloatArray(maxLength) { 0f }
        for (i in 0 until minOf(sequence.size, maxLength)) {
            padded[i] = sequence[i].toFloat()
        }

        return arrayOf(padded)
    }

    private fun loadModelFile(context: Context, filename: String): MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun loadTokenizerConfig(context: Context): Map<String, Int> {
        return try {
            val json = context.assets.open("tokenizer_config.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(json)
            val configObject = jsonObject.getJSONObject("config")
            val wordIndexObject = configObject.getJSONObject("word_index")

            val map = mutableMapOf<String, Int>()
            val keys = wordIndexObject.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = wordIndexObject.getInt(key)
            }
            map
        } catch (e: Exception) {
            Log.e("MLViewModel", "Error loading tokenizer: ${e.message}")
            emptyMap()
        }
    }

    fun resetState() {
        _mlState.value = MLState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        interpreter?.close()
    }
}