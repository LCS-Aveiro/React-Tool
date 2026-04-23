const USER_MODELS_KEY = 'rta_user_custom_models';

function updateExamplesDropdown() {
    const select = document.getElementById("examplesSelect");
    const currentValue = select.value;
    
    let allExamples = {};
    try {
        allExamples = JSON.parse(RTA.getExamples());
    } catch (e) { console.error("Erro ao carregar exemplos nativos", e); }

    let userModels = {};
    const saved = localStorage.getItem(USER_MODELS_KEY);
    if (saved) userModels = JSON.parse(saved);

    select.innerHTML = `<option value="">${i18n[currentLang].select_example}</option>`;

    const groupNative = document.createElement("optgroup");
    groupNative.label = currentLang === 'pt' ? "Exemplos Padrão" : "Built-in Examples";
    for (let name in allExamples) {
        let opt = document.createElement("option");
        opt.value = allExamples[name];
        opt.innerHTML = name;
        groupNative.appendChild(opt);
    }
    select.appendChild(groupNative);

    if (Object.keys(userModels).length > 0) {
        const groupUser = document.createElement("optgroup");
        groupUser.label = currentLang === 'pt' ? "Meus Modelos (Salvos)" : "My Saved Models";
        for (let name in userModels) {
            let opt = document.createElement("option");
            opt.value = userModels[name];
            opt.innerHTML = "⭐ " + name;
            opt.setAttribute('data-user-model', 'true');
            opt.setAttribute('data-raw-name', name);
            groupUser.appendChild(opt);
        }
        select.appendChild(groupUser);
    }
    
    select.value = currentValue;
}

function saveUserModel() {
    const name = prompt(currentLang === 'pt' ? "Nome do modelo:" : "Model name:");
    if (!name) return;

    let userModels = JSON.parse(localStorage.getItem(USER_MODELS_KEY) || '{}');
    userModels[name] = editor.getValue();
    
    localStorage.setItem(USER_MODELS_KEY, JSON.stringify(userModels));
    updateExamplesDropdown();
    alert(currentLang === 'pt' ? "Salvo com sucesso!" : "Saved successfully!");
}

function overwriteUserModel() {
    const select = document.getElementById("examplesSelect");
    const selectedOpt = select.options[select.selectedIndex];
    
    if (!selectedOpt || !selectedOpt.hasAttribute('data-user-model')) {
        alert(currentLang === 'pt' ? "Selecione um dos SEUS modelos para sobrescrever." : "Select one of YOUR models to overwrite.");
        return;
    }

    const name = selectedOpt.getAttribute('data-raw-name');
    let userModels = JSON.parse(localStorage.getItem(USER_MODELS_KEY) || '{}');
    userModels[name] = editor.getValue();
    
    localStorage.setItem(USER_MODELS_KEY, JSON.stringify(userModels));
    alert(currentLang === 'pt' ? "Modelo '" + name + "' atualizado!" : "Model '" + name + "' updated!");
}

function deleteUserModel() {
    const select = document.getElementById("examplesSelect");
    const selectedOpt = select.options[select.selectedIndex];
    
    if (!selectedOpt || !selectedOpt.hasAttribute('data-user-model')) {
        alert(currentLang === 'pt' ? "Você só pode excluir seus próprios modelos." : "You can only delete your own models.");
        return;
    }

    const name = selectedOpt.getAttribute('data-raw-name');
    if (confirm((currentLang === 'pt' ? "Excluir " : "Delete ") + name + "?")) {
        let userModels = JSON.parse(localStorage.getItem(USER_MODELS_KEY) || '{}');
        delete userModels[name];
        localStorage.setItem(USER_MODELS_KEY, JSON.stringify(userModels));
        updateExamplesDropdown();
    }
}


function createNewModel() {
    const name = prompt(currentLang === 'pt' ? "Nome do novo modelo:" : "New model name:");
    if (!name) return;

    const fileName = name.replace(".rta", "");
    const template = `name ${fileName}\ninit s0\ns0 ---> s1: a`;

    let userModels = JSON.parse(localStorage.getItem(USER_MODELS_KEY) || '{}');
    userModels[fileName] = template;
    localStorage.setItem(USER_MODELS_KEY, JSON.stringify(userModels));

    updateProjectTree();
    loadModelFromTree(template, fileName);
    
    document.getElementById('project-context-menu').style.display = 'none';
}