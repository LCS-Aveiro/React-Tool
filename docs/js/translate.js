const i18n = {
    pt: {
        subtitle: "Autômatos Reconfiguráveis: Análise Animada",
        tab_graph: "Grafo (Interativo)",
        tab_mermaid: "Mermaid",
        tab_text: "Texto",
        editor_title: "Editor",
        examples_label: "Exemplos:",
        select_example: "Selecione...",
        btn_load: "Carregar & Simular",
        export_title: "Exportar & Download",
        trans_title: "Tradução",
        btn_glts: "Traduzir para GLTS (Editor)",
        analysis_title: "Análise",
        btn_stats: "Estatísticas",
        btn_problems: "Buscar Problemas",
        m_full: "Passo Atual (Completo)",
        m_simple: "Passo Atual (Simples)",
        m_lts: "Todos os Passos (LTS)",
        panel_sim: "Simulação",
        msg_load_model: "Carregue um modelo para iniciar.",
        btn_undo: "Desfazer (Undo)",
        stat_header: "Estado Atual:",
        enabled_trans: "Transições Habilitadas:",
        deadlock: "DEADLOCK (Sistema Travado)",
        btn_delay: "⏱ Delay",
        panel_verif: "Verificação PDL",
        pdl_states: "1. Estados encontrados:",
        pdl_vars: "2. Variáveis (Dados):",
        pdl_elements: "3. Elementos para Fórmula:",
        btn_verify: "Verificar",
        pdl_placeholder: "Fórmula (Ex: <a>true)",
        pdl_st_placeholder: "Clique num estado acima...",
        layout_conf_title: "Configurações de Layout",
        layout_label: "Algoritmo de Layout:",
        edge_style_label: "Estilo das Linhas:",
        btn_save_layout: "Salvar Posições",
        btn_load_layout: "Carregar Posições",
        opt_preset: "Manual (Salvo)",
        opt_dagre: "Hierárquico (Dagre)",
        opt_cose: "Orgânico (Cose)",
        opt_circle: "Circular",
        opt_grid: "Grade",
        opt_random: "Aleatório",
        opt_straight: "Direto",
        opt_taxi: "Ângulos Retos (Taxi)",
        opt_bezier: "Curvas (Bezier)",
        modal_trans_title: "Detalhes da Transição",
        btn_close: "Fechar",
        alert_trans_ok: "Traduzido para GLTS com sucesso!",
        label_images: "Imagens:",
        btn_svg_mermaid: "Baixar SVG (Mermaid)",
        btn_png_cy: "Baixar PNG (Grafo)",
        msg_err_mermaid: "Por favor, clique na aba 'Mermaid' primeiro para gerar a visualização.",
        msg_err_load: "Carregue um modelo primeiro.",
        show_rules: "Mostrar Regras (Ativação/Desat.)",
    },
    en: {
        subtitle: "Reconfigurable Automata: Animated Analysis",
        tab_graph: "Graph (Interactive)",
        show_rules: "Show Rules (Act./Deact.)",
        tab_mermaid: "Mermaid",
        tab_text: "Text",
        editor_title: "Editor",
        examples_label: "Examples:",
        select_example: "Select...",
        btn_load: "Load & Simulate",
        export_title: "Export & Download",
        trans_title: "Translation",
        btn_glts: "Translate to GLTS (Editor)",
        analysis_title: "Analysis",
        btn_stats: "Statistics",
        btn_problems: "Check Problems",
        m_full: "Current Step (Full)",
        m_simple: "Current Step (Simple)",
        m_lts: "All Steps (LTS)",
        panel_sim: "Simulation",
        msg_load_model: "Load a model to start.",
        btn_undo: "Undo",
        stat_header: "Current State:",
        enabled_trans: "Enabled Transitions:",
        deadlock: "DEADLOCK / STUCK",
        btn_delay: "⏱ Delay",
        panel_verif: "PDL Verification",
        pdl_states: "1. States found:",
        pdl_vars: "2. Variables (Data):",
        pdl_elements: "3. Elements for Formula:",
        btn_verify: "Verify",
        pdl_placeholder: "Formula (e.g., <a>true)",
        pdl_st_placeholder: "Click a state above...",
        layout_conf_title: "Layout Settings",
        layout_label: "Layout Algorithm:",
        edge_style_label: "Edge Style:",
        btn_save_layout: "Save Positions",
        btn_load_layout: "Load Positions",
        opt_preset: "Manual (Saved)",
        opt_dagre: "Hierarchical (Dagre)",
        opt_cose: "Force-directed (Cose)",
        opt_circle: "Circular",
        opt_grid: "Grid",
        opt_random: "Random",
        opt_straight: "Straight",
        opt_taxi: "Orthogonal (Taxi)",
        opt_bezier: "Curved (Bezier)",
        modal_trans_title: "Transition Details",
        btn_close: "Close",
        alert_trans_ok: "Successfully translated to GLTS!",
        label_images: "Images:",
        btn_svg_mermaid: "Download SVG (Mermaid)",
        btn_png_cy: "Download PNG (Graph)",
        msg_err_mermaid: "Please click on the 'Mermaid' tab first to generate the view.",
        msg_err_load: "Load a model first.",
    }
};

const exampleDescriptions = {
    pt: {
        "Simple": "Demonstra a reconfiguração básica: a ação (a) desativa-se a si própria e depende de (b) para ser reativada.",
        "Conditions": "Introduz variáveis inteiras e guardas. O contador limita a execução de passos até um valor definido.",
        "LikeAlgorithm": "Modela um sistema de recomendação onde interações de Like/Dislike reconfiguram o que o utilizador vê.",
        "GRG": "Grafo Reativo Guardado complexo que utiliza flags de ativação para gerir estados de componentes.",
        "TIMER": "Exemplo fundamental de sistemas temporizados. Usa relógios e invariantes para forçar timeouts.",
        "Counter": "Utiliza regras de ativação em cascata para criar uma sequência lógica de passos progressivos.",
        "Penguim": "Modela hierarquias e exceções (Pinguins são pássaros que não voam) usando desativação.",
        "Vending (max eur1)": "Máquina de venda com exclusão mútua: inserir 1€ bloqueia a entrada de moedas de 50ct.",
        "Vending (max 3prod)": "Gestão de inventário: desativa opções de compra assim que o stock atinge o limite zero.",
        "Intrusive product": "Demonstra modularidade: um autómato mestre ativa transições num autómato escravo.",
        "Conflict": "Explora o comportamento do sistema quando existem regras contraditórias de ativação e desativação.",
        "Dependencies": "Usa a sintaxe de dependência forte (----> ) para ditar configurações entre componentes.",
        "Dynamic SPL": "Modela uma Linha de Produtos de Software que se reconfigura dinamicamente com base em funcionalidades."
    },
    en: {
        "Simple": "Demonstrates basic reconfiguration: action (a) disables itself and depends on (b) to be reactivated.",
        "Conditions": "Introduces integer variables and guards. The counter limits step execution to a defined value.",
        "LikeAlgorithm": "Models a recommendation system where Like/Dislike interactions reconfigure the user's view.",
        "GRG": "Complex Guarded Reactive Graph using activation flags to manage component states.",
        "TIMER": "Fundamental timed system example. Uses clocks and invariants to force timeouts.",
        "Counter": "Uses cascading activation rules to create a progressive logical sequence of steps.",
        "Penguim": "Models hierarchies and exceptions (Penguins are non-flying birds) using disabling.",
        "Vending (max eur1)": "Vending machine with mutual exclusion: inserting 1€ blocks 50ct coin entry.",
        "Vending (max 3prod)": "Inventory management: disables purchase options when stock reaches zero.",
        "Intrusive product": "Demonstrates modularity: a master automaton activates transitions in a slave automaton.",
        "Conflict": "Explores system behavior when contradictory activation and deactivation rules exist.",
        "Dependencies": "Uses strong dependency syntax (----> ) to dictate configurations between components.",
        "Dynamic SPL": "Models a Dynamic Software Product Line that reconfigures based on features at runtime."
    }
};


let currentLang = localStorage.getItem('rta_lang') || 'en';




function changeLanguage(lang) {
    currentLang = lang;
    localStorage.setItem('rta_lang', lang);
    applyTranslations();
}


function applyTranslations() {
    $('[data-i18n]').each(function () {
        const key = $(this).data('i18n');
        if (i18n[currentLang][key]) {
            $(this).text(i18n[currentLang][key]);
        }
    });

    $('#pdlFormula').attr('placeholder', currentLang === 'pt' ? 'Fórmula (Ex: <a>true)' : 'Formula (Ex: <a>true)');

    if (document.getElementById("examplesSelect").value !== "") {
        loadExample();
    }

}

$(document).ready(function () {
    applyTranslations();
});
