

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


function findPathByValue() {
    const cond = document.getElementById('pathTargetCond').value;
    const resultDiv = document.getElementById('pathResult');
    
    if (!cond) {
        alert("Digite uma condição (ex: wolf == 1)");
        return;
    }

    resultDiv.innerHTML = "<i>Analisando variáveis...</i>";
    console.log(cond)
    const response = RTA.findPathToValue(cond);
    console.log(response)
    if (response.error){
        resultDiv.innerHTML = `<span class="text-danger">${response.error}</span>`;
        return;
    }

    const pdlSeq = `⟨${response.join(';')}⟩true`;
    resultDiv.innerHTML = `
        <b>Caminho para atingir ${cond}:</b><br>
        <div class="input-group input-group-sm">
            <input type="text" class="form-control" value="${pdlSeq}" readonly>
            <span class="input-group-btn">
                <button class="btn btn-info" onclick="useInPdl('${pdlSeq}')">Ir</button>
            </span>
        </div>
    `;


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
