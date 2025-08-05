import tensorflow as tf
import os

# Check the first conv layer of the model
model_path = 'src/main/resources/models/2D/he_heavy_augment'
if os.path.exists(model_path):
    try:
        model = tf.saved_model.load(model_path)
        
        # Get the concrete function
        concrete_func = model.signatures['serving_default']
        
        # Try to get the graph
        graph_def = concrete_func.graph.as_graph_def()
        
        print('=== FIRST CONV LAYER INFO ===')
        for node in graph_def.node:
            if 'conv2d' in node.name.lower() and ('kernel' in node.name or 'weight' in node.name):
                print(f'Node: {node.name}')
                print(f'Op: {node.op}')
                if hasattr(node, 'attr') and 'shape' in node.attr:
                    print(f'Shape: {node.attr["shape"]}')
                break
                
        # Look for the first conv2d operation
        for node in graph_def.node:
            if node.op == 'Conv2D' and 'conv2d_1' in node.name:
                print(f'Conv2D node: {node.name}')
                print(f'Inputs: {node.input}')
                break
                
        # Check variables for conv2d_1 kernel
        print('\n=== VARIABLES ===')
        for var in tf.saved_model.load(model_path).variables:
            if 'conv2d_1' in var.name and 'kernel' in var.name:
                print(f'Variable: {var.name}')
                print(f'Shape: {var.shape}')
                print(f'First few values: {var.numpy().flatten()[:10]}')
                break
                
    except Exception as e:
        print(f'Error: {e}')
        import traceback
        traceback.print_exc()
else:
    print(f'Model path not found: {model_path}')