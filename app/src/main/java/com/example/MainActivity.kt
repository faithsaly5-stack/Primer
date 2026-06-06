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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.content.Intent
import android.net.Uri
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
                title = { Text("PrimerPro", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Science, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("Primer Design Engine", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Developed by Shayan Yegan Nejad", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(56.dp))
            
            DashboardCard(
                title = "Online NCBI Mode",
                subtitle = "Fetch & design from random NCBI gene",
                icon = Icons.Filled.Language,
                onClick = { navController.navigate("online") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DashboardCard(
                title = "Offline Pre-loaded Mode",
                subtitle = "Fast local calculation for common genes",
                icon = Icons.Filled.List,
                onClick = { navController.navigate("offline") }
            )
            Spacer(modifier = Modifier.height(16.dp))
            DashboardCard(
                title = "Manual Input Mode",
                subtitle = "Paste any sequence for instant design",
                icon = Icons.Filled.Edit,
                onClick = { navController.navigate("manual") }
            )
        }
    }
}

@Composable
fun DashboardCard(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineModeScreen(navController: NavController) {
    var isLoading by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("") }
    var fetchedId by remember { mutableStateOf<String?>(null) }
    var fastaHeader by remember { mutableStateOf("") }
    var rawSeq by remember { mutableStateOf("") }
    var resultPair by remember { mutableStateOf<PrimerDesignEngine.PrimerPair?>(null) }
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
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Fetch a random gene from NCBI to design primers.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    resultPair = null
                    fetchedId = null
                    statusText = "Starting..."
                    scope.launch {
                        try {
                            val randomIds = listOf("NM_002046.7", "NM_001101.5", "NM_000546.6", "NM_001126.3")
                            val id = randomIds.random()
                            statusText = "Connecting to NCBI Entrez to fetch $id..."
                            
                            val response = withContext(Dispatchers.IO) {
                                NCBIClient.api.fetchFasta(id).string()
                            }
                            
                            statusText = "Parsing FASTA sequence..."
                            val lines = response.split("\n")
                            val header = lines.firstOrNull() ?: ">Unknown"
                            val seq = lines.drop(1).joinToString("")
                            
                            withContext(Dispatchers.Main) {
                                fetchedId = id
                                fastaHeader = header
                                rawSeq = seq
                                isLoading = false
                                statusText = ""
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                isLoading = false
                                statusText = ""
                                Toast.makeText(context, "Network Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Fetch Random NCBI Gene", fontWeight = FontWeight.Bold)
            }
            
            if (statusText.isNotEmpty() && fetchedId == null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(statusText, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                }
            }
            
            if (fetchedId != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Gene Sequence Found", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(fastaHeader, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Sequence (${rawSeq.length} bp):", fontWeight = FontWeight.Bold)
                        Text(rawSeq.take(100) + "...", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Reference Link (Screenshot this):", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                        Text("https://www.ncbi.nlm.nih.gov/nuccore/$fetchedId", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = {
                            val url = "https://www.ncbi.nlm.nih.gov/tools/primer-blast/index.cgi?INPUT_SEQUENCE=${Uri.encode(rawSeq)}"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("Design via\nPrimer-BLAST", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                    
                    Button(
                        onClick = {
                            statusText = "Running local engine..."
                            scope.launch {
                                val pair = withContext(Dispatchers.Default) {
                                    PrimerDesignEngine.designPrimers(rawSeq)
                                }
                                withContext(Dispatchers.Main) {
                                    resultPair = pair
                                    statusText = ""
                                    if (pair == null) Toast.makeText(context, "Could not find valid primers", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Calculate\nLocally", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
            
            if (statusText.isNotEmpty() && fetchedId != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(statusText, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSecondaryContainer, fontWeight = FontWeight.SemiBold)
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            resultPair?.let {
                PrimerResultCard(it, fetchedId ?: "Unknown Gene", rawSeq, context)
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
    var isLoading by remember { mutableStateOf(false) }
    var rawSeq by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Offline Mode") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Select a pre-loaded gene signature to design primers locally without internet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedGene,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Gene") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    HardcodedSequences.names.forEach { selectionOption ->
                        DropdownMenuItem(
                            text = { Text(selectionOption, fontWeight = FontWeight.Medium) },
                            onClick = {
                                selectedGene = selectionOption
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    val seq = HardcodedSequences.sequences[selectedGene]
                    if (seq != null) {
                        isLoading = true
                        resultPair = null
                        rawSeq = seq
                        scope.launch {
                            val pair = withContext(Dispatchers.Default) {
                                PrimerDesignEngine.designPrimers(seq)
                            }
                            resultPair = pair
                            isLoading = false
                            if (pair == null) {
                                Toast.makeText(context, "No valid primers found.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Please select a gene.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Design Primers", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            resultPair?.let {
                PrimerResultCard(it, selectedGene, rawSeq, context)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualInputScreen(navController: NavController) {
    var rawSequence by remember { mutableStateOf("") }
    var resultPair by remember { mutableStateOf<PrimerDesignEngine.PrimerPair?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Manual Input Mode") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 24.dp).verticalScroll(rememberScrollState())
        ) {
            Text("Paste a FASTA or raw nucleotide sequence here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            
            OutlinedTextField(
                value = rawSequence,
                onValueChange = { rawSequence = it },
                modifier = Modifier.fillMaxWidth().height(250.dp),
                label = { Text("Target Sequence") },
                shape = RoundedCornerShape(12.dp),
                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    isLoading = true
                    resultPair = null
                    scope.launch {
                        // Quick rough extraction if it is FASTA
                        val cleanSeq = if (rawSequence.startsWith(">")) {
                            rawSequence.split("\n", limit = 2).getOrNull(1)?.replace("\n", "") ?: rawSequence
                        } else {
                            rawSequence
                        }
                        val pair = withContext(Dispatchers.Default) {
                            PrimerDesignEngine.designPrimers(cleanSeq)
                        }
                        resultPair = pair
                        isLoading = false
                        if (pair == null) Toast.makeText(context, "No valid primers found.", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                else Text("Generate Primers", fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            resultPair?.let {
                PrimerResultCard(it, "Manual Input", rawSequence, context)
            }
        }
    }
}

@Composable
fun PrimerResultCard(pair: PrimerDesignEngine.PrimerPair, geneName: String, seq: String, context: Context) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Results for $geneName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Product Size: ${pair.productSize} bp", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Forward Primer Block
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Forward Primer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("5' - ${pair.forward.sequence} - 3'", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Tm: %.1f °C".format(pair.forward.tm), style = MaterialTheme.typography.bodyMedium)
                        Text("GC: %.1f%%".format(pair.forward.gcPercent), style = MaterialTheme.typography.bodyMedium)
                        Text("Len: ${pair.forward.length}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reverse Primer Block
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                    Text("Reverse Primer", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("5' - ${pair.reverse.sequence} - 3'", fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Tm: %.1f °C".format(pair.reverse.tm), style = MaterialTheme.typography.bodyMedium)
                        Text("GC: %.1f%%".format(pair.reverse.gcPercent), style = MaterialTheme.typography.bodyMedium)
                        Text("Len: ${pair.reverse.length}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { ExportModule.shareAsText(context, geneName, pair.forward.sequence, pair.reverse.sequence, pair.forward.tm, pair.reverse.tm, pair.forward.gcPercent, pair.reverse.gcPercent, pair.productSize) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Report", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("TXT")
                }
                Button(
                    onClick = { ExportModule.shareAsPdf(context, geneName, pair.forward.sequence, pair.reverse.sequence, pair.forward.tm, pair.reverse.tm, pair.forward.gcPercent, pair.reverse.gcPercent, pair.productSize) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "PDF", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("PDF")
                }
                OutlinedButton(
                    onClick = { ExportModule.shareGeneText(context, geneName, seq) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.FileDownload, contentDescription = "Gene", modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Gene")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = {
                    val url = "https://www.ncbi.nlm.nih.gov/tools/primer-blast/index.cgi?INPUT_SEQUENCE=${Uri.encode(seq)}&PRIMER_LEFT_INPUT=${Uri.encode(pair.forward.sequence)}&PRIMER_RIGHT_INPUT=${Uri.encode(pair.reverse.sequence)}"
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Icon(Icons.Filled.OpenInNew, contentDescription = "Primer-BLAST", modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Verify Specificity in Primer-BLAST", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiary)
            }
        }
    }
}
