package com.example

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.primerpro.ExportModule
import com.example.primerpro.HardcodedSequences
import com.example.primerpro.NCBIClient
import com.example.primerpro.PrimerDesignEngine
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainNavigation()
            }
        }
    }
}

@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "home") {
        composable("home") { HomeScreen(navController) }
        composable("online") { OnlineModeScreen(navController) }
        composable("offline") { OfflineModeScreen(navController) }
        composable("manual") { ManualInputScreen(navController) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PrimerPro") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Primer Design Engine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Developed by Shayan Yegan Nejad", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(48.dp))
            
            Button(onClick = { navController.navigate("online") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Online Random NCBI Mode")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("offline") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Offline Mode (Pre-loaded)")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { navController.navigate("manual") }, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Text("Manual Input Mode")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineModeScreen(navController: NavController) {
    var isLoading by remember { mutableStateOf(false) }
    var resultPair by remember { mutableStateOf<PrimerDesignEngine.PrimerPair?>(null) }
    var geneFound by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Online Random NCBI") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Fetches a random gene from NCBI and designs primers.")
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    resultPair = null
                    scope.launch {
                        try {
                            // Pick a random notable NCBI ID for demo purposes to avoid huge data downloads
                            val randomIds = listOf("NM_002046.7", "NM_001101.5", "NM_000546.6", "NM_001126.3")
                            val id = randomIds.random()
                            geneFound = "Gene Accession: $id"
                            
                            val response = withContext(Dispatchers.IO) {
                                NCBIClient.api.fetchFasta(id).string()
                            }
                            
                            // Extract sequence from FASTA (skip line 1)
                            val seq = response.split("\n").drop(1).joinToString("")
                            
                            val pair = PrimerDesignEngine.designPrimers(seq)
                            withContext(Dispatchers.Main) {
                                resultPair = pair
                                isLoading = false
                                if (pair == null) Toast.makeText(context, "Could not find valid primers for this gene", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Fetch & Design")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            resultPair?.let {
                PrimerResultCard(it, geneFound, context)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OfflineModeScreen(navController: NavController) {
    var expanded by remember { mutableStateOf(false) }
    var selectedGene by remember { mutableStateOf("") }
    var resultPair by remember { mutableStateOf<PrimerDesignEngine.PrimerPair?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Mode") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Select a pre-loaded gene signature to design primers locally.")
            Spacer(modifier = Modifier.height(16.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                TextField(
                    value = selectedGene,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Gene") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    HardcodedSequences.names.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption) },
                            onClick = {
                                selectedGene = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val seq = HardcodedSequences.sequences[selectedGene]
                    if (seq != null) {
                        resultPair = PrimerDesignEngine.designPrimers(seq)
                        if (resultPair == null) {
                            Toast.makeText(context, "No valid primers found.", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Please select a gene.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Design Primers")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            resultPair?.let {
                PrimerResultCard(it, selectedGene, context)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputScreen(navController: NavController) {
    var rawSequence by remember { mutableStateOf("") }
    var resultPair by remember { mutableStateOf<PrimerDesignEngine.PrimerPair?>(null) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Input Mode") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Paste a FASTA or raw nucleotide sequence here.")
            Spacer(modifier = Modifier.height(8.dp))
            
            OutlinedTextField(
                value = rawSequence,
                onValueChange = { rawSequence = it },
                modifier = Modifier.fillMaxWidth().height(200.dp),
                label = { Text("Sequence") }
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    // Quick rough extraction if it is FASTA
                    val cleanSeq = if (rawSequence.startsWith(">")) {
                        rawSequence.split("\n", limit = 2).getOrNull(1)?.replace("\n", "") ?: rawSequence
                    } else {
                        rawSequence
                    }
                    val pair = PrimerDesignEngine.designPrimers(cleanSeq)
                    resultPair = pair
                    if (pair == null) Toast.makeText(context, "No valid primers found.", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text("Generate Primers")
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            resultPair?.let {
                PrimerResultCard(it, "Manual Input", context)
            }
        }
    }
}

@Composable
fun PrimerResultCard(pair: PrimerDesignEngine.PrimerPair, geneName: String, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Results for $geneName", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Product Size: ${pair.productSize} bp")
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            
            Text("Forward Primer", fontWeight = FontWeight.Bold)
            Text("5' - ${pair.forward.sequence} - 3'", color = MaterialTheme.colorScheme.primary)
            Text("Tm: %.2f °C | GC: %.1f %% | Length: %d bp".format(pair.forward.tm, pair.forward.gcPercent, pair.forward.length))
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text("Reverse Primer", fontWeight = FontWeight.Bold)
            Text("5' - ${pair.reverse.sequence} - 3'", color = MaterialTheme.colorScheme.primary)
            Text("Tm: %.2f °C | GC: %.1f %% | Length: %d bp".format(pair.reverse.tm, pair.reverse.gcPercent, pair.reverse.length))
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    ExportModule.shareAsText(context, geneName, pair.forward.sequence, pair.reverse.sequence, pair.forward.tm, pair.reverse.tm, pair.forward.gcPercent, pair.reverse.gcPercent, pair.productSize)
                }) {
                    Icon(Icons.Filled.Share, contentDescription = "Share Text")
                    Spacer(Modifier.width(4.dp))
                    Text("Share TXT")
                }
                Button(onClick = {
                    ExportModule.shareAsPdf(context, geneName, pair.forward.sequence, pair.reverse.sequence, pair.forward.tm, pair.reverse.tm, pair.forward.gcPercent, pair.reverse.gcPercent, pair.productSize)
                }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "Share PDF")
                    Spacer(Modifier.width(4.dp))
                    Text("Share PDF")
                }
            }
        }
    }
}
