

const stripNameCommand = s => {
    return s.replace(/^\s*name\s+[a-zA-Z0-9_]+[;\s]*/gm, '');
};


function loadExample() {
    var select = document.getElementById("examplesSelect");
    var code = select.value;
    var descDiv = document.getElementById("exampleDesc");

    var selectedName = select.options[select.selectedIndex].text;

    if (code) {
        editor.setValue(code);

        const desc = exampleDescriptions[currentLang][selectedName];

        if (desc) {
            descDiv.innerText = desc;
            descDiv.style.display = "block";
        } else {
            descDiv.style.display = "none";
        }
    } else {
        descDiv.style.display = "none";
    }
}


function loadAndRender() {
    var fullCode = editor.getValue();
    var cleanCode = stripNameCommand(fullCode);
    console.log(fullCode);
    console.log(cleanCode);

    var jsonString = RTA.loadModel(cleanCode);
    var data = JSON.parse(jsonString);

    if (data.error) {
        alert(data.error);
    } else {
        textTraceHistory = [];
        jsTextHistory = [];
        var initialStateText = RTA.getCurrentStateText();
        jsTextHistory.push({ label: "Start ->", text: initialStateText });
        renderCytoscapeGraph("cytoscapeMainContainer", data, true);

        updateAllViews(jsonString);
        console.log(data);
        renderPdlHelpers(data);
    }
}



function updateProjectTree() {
    const tree = document.getElementById("project-tree");
    if (!tree) return;
    tree.innerHTML = "";

    let nativeExamples = {};
    try {
        nativeExamples = JSON.parse(RTA.getExamples());
    } catch (e) { console.error(e); }

    let userModels = {};
    const saved = localStorage.getItem('rta_user_custom_models');
    if (saved) userModels = JSON.parse(saved);

    renderTreeSection(tree, "Examples", nativeExamples, "📁", false);

    if (Object.keys(userModels).length > 0) {
        renderTreeSection(tree, "My Models", userModels, "⭐", true);
    }
}

function renderTreeSection(container, title, models, icon, isUser) {
    const header = document.createElement("div");
    header.className = "tree-item";
    header.style.fontWeight = "bold";
    header.style.color = "var(--gray-500)";
    header.innerHTML = `<span class="tree-icon">${icon}</span> ${title}`;
    container.appendChild(header);

    for (let name in models) {
        const item = document.createElement("div");
        item.className = "tree-item tree-indent";
        item.innerHTML = `<span class="tree-icon">📄</span> ${name}.rta`;
        
        item.onclick = function() {
            selectTreeItem(item);
            loadModelFromTree(models[name], name);
        };
        
        container.appendChild(item);
    }
}

function loadModelFromTree(code, name) {
    editor.setValue(code);
    if (typeof exampleDescriptions !== 'undefined') {
        const descDiv = document.getElementById("exampleDesc");
        const desc = exampleDescriptions[currentLang][name];
        if (desc) {
            descDiv.innerText = desc;
            descDiv.style.display = "block";
        } else {
            descDiv.style.display = "none";
        }
    }
    showCanvasTab('editorTab');
    
    document.getElementById('sb-model').textContent = name + ".rta";
}

function selectTreeItem(element) {
    document.querySelectorAll('.tree-item').forEach(el => el.classList.remove('selected'));
    element.classList.add('selected');
}


function updatePathfindingDropdown(data) {
    const select = document.getElementById('pathEndState');
    if (!select) return;
    
    const currentSelection = select.value;
    
    select.innerHTML = '<option value="">State...</option>';
    

    const states = data.graphElements
        .filter(el => el.classes && el.classes.includes('state-node'))
        .map(el => el.data.id);

    const uniqueStates = [...new Set(states)].sort();

    uniqueStates.forEach(id => {
        const opt = document.createElement('option');
        opt.value = id;
        opt.text = id;
        select.appendChild(opt);
    });

    select.value = currentSelection;
}


function findBestPath() {
    const targetId = document.getElementById('pathEndState').value;
    const resultDiv = document.getElementById('pathResult');
    
    if (!targetId) {
        alert("Select a destination.");
        return;
    }

    resultDiv.innerHTML = "";

    const response = RTA.findBestPath(targetId);
    
    try {
        const data = JSON.parse(response);
        
        if (data.error) {
            resultDiv.innerHTML = `<span class="text-danger">${data.error}</span>`;
        } else {
            if (Array.isArray(data)) {
                if (data.length === 0) {
                    resultDiv.innerHTML = "<span class='text-success'>You're already at your destination!</span>";
                } else {
                    const pdlSequence = `⟨${data.join(';')}⟩true`;
        
                    resultDiv.innerHTML = `
                        <b>Best way (PDL):</b><br>
                        <div class="input-group input-group-sm" style="margin-top:5px;">
                            <input type="text" id="generatedPdlPath" class="form-control" value="${pdlSequence}" readonly>
                            <span class="input-group-btn">
                                <button class="btn btn-info" onclick="useInPdl('${pdlSequence}')" title="Usar na Verificação">
                                    <span class="glyphicon glyphicon-share-alt"></span>
                                </button>
                            </span>
                        </div>
                    `;
                }
            }
        }
    } catch(e) {
        resultDiv.innerHTML = "Error processing Scala response.";
    }
}


function updateBPField() {
    const type = document.getElementById('bpType').value;
    const label = document.getElementById('bpValueLabel');
    const input = document.getElementById('bpValue');
    const intContainer = document.getElementById('bpIntContainer');

    if (type === 'state') {
        label.innerText = "Target State";
        input.placeholder = "e.g. s1";
        intContainer.style.display = 'none';
    } else {
        label.innerText = "Condition (Boolean Expression)";
        input.placeholder = "e.g. u1==0 && u2==0 && d1==1";
        // Se você quiser digitar a expressão toda no campo acima, 
        // mantemos o intContainer escondido.
        intContainer.style.display = 'none'; 
    }
}

function findPathByValue() {
    const type = document.getElementById('bpType').value;
    const targetValue = document.getElementById('bpValue').value.trim();
    const summaryDiv = document.getElementById('bpResultSummary');
    const pathDiv = document.getElementById('bpResultPath');
    const resultBox = document.getElementById('bpResultBox');

    if (!targetValue) {
        alert("Please enter a target (state name or condition).");
        return;
    }

    // Mostrar a caixa e limpar resultados anteriores
    resultBox.style.display = 'block';
    summaryDiv.innerHTML = "Analyzing model...";
    summaryDiv.style.color = "var(--gray-700)";
    pathDiv.innerHTML = "";

    let response;
    
    if (type === 'state') {
        // Busca caminho para um estado específico (ex: "s1")
        response = RTA.findBestPath(targetValue);
    } else {
        // Busca caminho para uma expressão complexa (ex: "u1==0 && u2==0")
        response = RTA.findPathToValue(targetValue);
    }

    try {
        // O Scala retorna uma string JSON que precisamos converter
        const data = (typeof response === 'string') ? JSON.parse(response) : response;

        if (data.error) {
            summaryDiv.innerHTML = "No path found";
            summaryDiv.style.color = "var(--red)";
            pathDiv.innerHTML = `<span style="font-size:11px; color:var(--gray-500)">${data.error}</span>`;
        } else if (Array.isArray(data)) {
            if (data.length === 0) {
                summaryDiv.innerHTML = "Target already reached!";
                summaryDiv.style.color = "#16a34a";
            } else {
                const pdlSequence = `⟨${data.join('; ')}⟩true`;
                summaryDiv.innerHTML = `Path found! (${data.length} steps)`;
                summaryDiv.style.color = "#2563eb";
                
                pathDiv.innerHTML = `
                    <div style="background:#f1f5f9; padding:10px; border:1px solid #e2e8f0; border-radius:4px; margin-top:5px;">
                        <code style="display:block; margin-bottom:8px; word-break: break-all; color:#334155;">${pdlSequence}</code>
                        <button class="u-btn primary" style="padding:2px 8px; font-size:10px;" 
                                onclick="useInPdl('${pdlSequence}')">
                            <span class="glyphicon glyphicon-share-alt"></span> Send to PDL Verifier
                        </button>
                    </div>
                `;
            }
        }
    } catch (e) {
        console.error("Analysis error:", e);
        summaryDiv.innerHTML = "Error processing engine response.";
        summaryDiv.style.color = "var(--red)";
    }
}

function useInPdl(formula) {
    const pdlInput = document.getElementById('pdlFormula');
    if (pdlInput) {
        pdlInput.value = formula;
        
        pdlInput.style.backgroundColor = "#d9edf7";
        setTimeout(() => pdlInput.style.backgroundColor = "#fff", 500);
        
        pdlInput.focus();
    } else {
        alert("Campo PDL não encontrado!");
    }
}

$(document).ready(function() {
    $('#simCollapse, #pdlBody').on('shown.bs.collapse hidden.bs.collapse', function () {
        $(this).css('height', '');
    });
});

$(document).on('shown.bs.tab', 'a[data-toggle="tab"]', function (e) {
    var target = $(e.target).attr("href");

    if (target === '#mermaidTab') {
        setTimeout(function () {
            renderMermaidView();
        }, 10);
    }

    
});
